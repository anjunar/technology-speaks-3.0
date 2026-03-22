package jFx2.core.rendering

import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.dom.RangeInsertPoint
import jFx2.core.dom.moveRangeInclusive
import jFx2.core.runtime.ComponentMount
import jFx2.core.runtime.componentWithScope
import jFx2.state.Disposable
import jFx2.state.JobRegistry
import jFx2.state.ListChange
import jFx2.state.ListProperty
import jFx2.state.Property
import kotlinx.browser.document
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import org.w3c.dom.Comment
import org.w3c.dom.DocumentFragment
import org.w3c.dom.Node
import kotlin.random.Random

private data class JobRangeItemMount(
    val key: Any,
    val start: Comment,
    val end: Comment,
    val range: RangeInsertPoint,
    val owner: Component<*>,
    val scope: NodeScope,              // stable per-item scope
    var mount: ComponentMount,
    val index: Property<Int>,
    var job: Job? = null,
    var disposed: Boolean = false
)

private fun lisKeepMask(seq: IntArray): BooleanArray {
    val n = seq.size
    if (n == 0) return BooleanArray(0)

    val prev = IntArray(n) { -1 }
    val tailIdx = IntArray(n)
    val tailVal = IntArray(n)
    var length = 0

    for (i in 0 until n) {
        val x = seq[i]
        var lo = 0
        var hi = length
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (tailVal[mid] < x) lo = mid + 1 else hi = mid
        }
        val pos = lo
        if (pos > 0) prev[i] = tailIdx[pos - 1]
        tailIdx[pos] = i
        tailVal[pos] = x
        if (pos == length) length++
    }

    val keep = BooleanArray(n)
    var k = tailIdx[length - 1]
    while (k >= 0) {
        keep[k] = true
        k = prev[k]
    }
    return keep
}

private fun newToken(prefix: String): Any = "$prefix-${Random.nextInt()}"

private class ForeachAsyncComponent<T>(
    override val node: DocumentFragment,
    private val start: Comment,
    private val end: Comment,
    private val items: ListProperty<T>,
    private val keyOf: (T) -> Any,
    private val block: suspend context(NodeScope) (T, Property<Int>) -> Unit
) : Component<DocumentFragment>() {

    private var disposed = false
    private var baseScope: NodeScope? = null
    private var committedRange: RangeInsertPoint? = null

    private val mounts = LinkedHashMap<Any, JobRangeItemMount>()

    private val foreachOwner = newToken("foreach")
    private val jobs = JobRegistry.instance

    override fun mount() {
        with(baseScope!!) {
            reconcile(items.get(), restartJobs = true)
        }
    }

    context(scope: NodeScope)
    fun init() {
        baseScope = scope

        val d: Disposable = items.observeChanges { ch ->
            when (ch) {
                is ListChange.Add,
                is ListChange.Remove -> scheduleReconcile(restartJobs = false)

                is ListChange.Replace,
                is ListChange.SetAll -> scheduleReconcile(restartJobs = true)

                is ListChange.Clear -> scheduleClear()
            }
        }
        onDispose(d)
    }

    private fun ensureRangeCommitted(): RangeInsertPoint {
        committedRange?.let { return it }
        return RangeInsertPoint(start, end).also { committedRange = it }
    }

    private fun scheduleReconcile(restartJobs: Boolean) {
        val scope = baseScope ?: return
        if (disposed) return

        with(scope) { reconcile(items.get(), restartJobs) }
    }

    private fun scheduleClear() {
        val scope = baseScope ?: return
        if (disposed) return

        scope.ui.build.afterBuild {
            if (disposed) return@afterBuild
            val range = ensureRangeCommitted()

            // kill all jobs belonging to this foreach instance
            runCatching { jobs.cancelAllFor(foreachOwner) }

            for ((_, im) in mounts) disposeAndRemove(im)
            mounts.clear()
            range.clear()
        }
    }

    private fun disposeAndRemove(im: JobRangeItemMount) {
        im.disposed = true
        runCatching { im.job?.cancel() }
        im.job = null
        runCatching { im.mount.dispose() }
        runCatching { im.range.dispose() }
    }

    private fun startAsync(parentScope: NodeScope, im: JobRangeItemMount, item: T) {
        // cancel old job
        im.job?.cancel()

        val label = "foreachAsync[item=${im.key}]"
        im.job = jobs.launch(label = label, owner = foreachOwner) {
            try {
                with(im.scope) { block(item, im.index) }
                im.scope.ui.build.flush()
            } catch (_: CancellationException) {
                // ignore
            } catch (_: Throwable) {
                // optional logging
            }
        }

        // item should also die with parent scope
        parentScope.dispose.register { im.job?.cancel() }
    }

    context(scope: NodeScope)
    private fun mountItem(item: T, idx: Int, k: Any, hostRange: RangeInsertPoint): JobRangeItemMount {
        val itemStart = document.createComment("jFx2:item")
        val itemEnd = document.createComment("jFx2:/item")
        hostRange.insert(itemStart)
        hostRange.insert(itemEnd)

        val itemRange = RangeInsertPoint(itemStart, itemEnd)
        val owner = RangeOwner(itemStart)
        val indexProp = Property(idx)

        val itemScope = scope.fork(
            parent = itemRange.parent,
            owner = owner,
            ctx = scope.ctx.fork(),
            insertPoint = itemRange
        )
        itemScope.dispose.register { itemRange.dispose() }

        val m = componentWithScope(itemScope) {
            // placeholder (optional)
        }

        val im = JobRangeItemMount(
            key = k,
            start = itemStart,
            end = itemEnd,
            range = itemRange,
            owner = owner,
            scope = itemScope,
            mount = m,
            index = indexProp
        )

        startAsync(scope, im, item)
        return im
    }

    context(scope: NodeScope)
    private fun reconcile(newItems: List<T>, restartJobs: Boolean) {
        if (disposed) return

        val hostRange = ensureRangeCommitted()
        val parent: Node = start.parentNode ?: return

        // 1) new key order
        val newKeys = ArrayList<Any>(newItems.size)
        for (it in newItems) newKeys += keyOf(it)
        val newKeySet = newKeys.toHashSet()

        // 2) remove missing
        run {
            val iter = mounts.entries.iterator()
            while (iter.hasNext()) {
                val (k, im) = iter.next()
                if (k !in newKeySet) {
                    disposeAndRemove(im)
                    iter.remove()
                }
            }
        }

        // 3) add missing
        for (i in newItems.indices) {
            val k = newKeys[i]
            if (!mounts.containsKey(k)) {
                mounts[k] = mountItem(newItems[i], i, k, hostRange)
            }
        }

        // 4) reorder with LIS
        val startToKey = HashMap<Node, Any>(mounts.size * 2)
        for ((k, im) in mounts) startToKey[im.start] = k

        val currentKeys = ArrayList<Any>(mounts.size)
        run {
            var n: Node? = start.nextSibling
            while (n != null && n != end) {
                val k = startToKey[n]
                if (k != null) currentKeys += k
                n = n.nextSibling
            }
        }

        val currentIndex = HashMap<Any, Int>(currentKeys.size * 2)
        for (i in currentKeys.indices) currentIndex[currentKeys[i]] = i

        val seq = IntArray(newKeys.size)
        for (i in newKeys.indices) {
            seq[i] = currentIndex[newKeys[i]] ?: Int.MAX_VALUE
        }

        val keep = lisKeepMask(seq)

        var before: Node? = end
        for (i in newKeys.indices.reversed()) {
            val k = newKeys[i]
            val im = mounts.getValue(k)
            if (!keep[i]) {
                moveRangeInclusive(parent, im.start, im.end, before)
            }
            before = im.start
        }

        // 5) update indices + optional restart
        for (i in newItems.indices) {
            val k = newKeys[i]
            val im = mounts.getValue(k)
            im.index.set(i)
            if (restartJobs) startAsync(scope, im, newItems[i])
        }

        // 6) align map order
        if (mounts.size == newKeys.size) {
            val rebuilt = LinkedHashMap<Any, JobRangeItemMount>(mounts.size * 2)
            for (k in newKeys) rebuilt[k] = mounts.getValue(k)
            mounts.clear()
            mounts.putAll(rebuilt)
        }
    }

    override fun dispose() {
        disposed = true

        runCatching { jobs.cancelAllFor(foreachOwner) }

        for ((_, im) in mounts) runCatching { disposeAndRemove(im) }
        mounts.clear()

        runCatching { committedRange?.dispose() }
        committedRange = null

        runCatching { end.parentNode?.removeChild(end) }

        super.dispose()
    }
}

context(scope: NodeScope)
fun <T> foreachAsync(
    items: ListProperty<T>,
    key: (T) -> Any,
    block: suspend context(NodeScope) (T, Property<Int>) -> Unit
) {
    val start: Comment = document.createComment("jFx2:foreach")
    val end: Comment = document.createComment("jFx2:/foreach")

    val fragment = document.createDocumentFragment()
    fragment.appendChild(start)
    fragment.appendChild(end)

    val comp = ForeachAsyncComponent(
        node = fragment,
        start = start,
        end = end,
        items = items,
        keyOf = key,
        block = block
    )

    scope.attach(comp)
    with(scope) { comp.init() }
}
