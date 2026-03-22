package jFx2.core.rendering

import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.dom.RangeInsertPoint
import jFx2.core.dom.moveRangeInclusive
import jFx2.core.runtime.ComponentMount
import jFx2.core.runtime.componentWithScope
import jFx2.state.Disposable
import jFx2.state.ListChange
import jFx2.state.ListProperty
import jFx2.state.Property
import kotlinx.browser.document
import org.w3c.dom.Comment
import org.w3c.dom.DocumentFragment
import org.w3c.dom.Node

private data class RangeItemMount(
    val key: Any,
    val start: Comment,
    val end: Comment,
    val range: RangeInsertPoint,
    val owner: Component<*>,
    val scope: NodeScope,              // stable per-item scope
    var mount: ComponentMount,
    val index: Property<Int>
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

private class ForeachComponent<T>(
    override val node: DocumentFragment,
    private val start: Comment,
    private val end: Comment,
    private val items: ListProperty<T>,
    private val keyOf: (T) -> Any,
    private val block: context(NodeScope) (T, Property<Int>) -> Unit
) : Component<DocumentFragment>() {

    private var disposed = false
    private var baseScope: NodeScope? = null
    private var committedRange: RangeInsertPoint? = null

    private val mounts = LinkedHashMap<Any, RangeItemMount>()

    override fun mount() {
        with(baseScope!!) {
            reconcile(items.get())
        }
    }

    context(scope: NodeScope)
    fun init() {
        baseScope = scope

        val d: Disposable = items.observeChanges { ch ->
            when (ch) {
                is ListChange.Add,
                is ListChange.Remove,
                is ListChange.Replace,
                is ListChange.SetAll -> scheduleReconcile()

                is ListChange.Clear -> scheduleClear()
            }
        }
        onDispose(d)
    }

    private fun ensureRangeCommitted(): RangeInsertPoint {
        committedRange?.let { return it }
        return RangeInsertPoint(start, end).also { committedRange = it }
    }

    private fun scheduleReconcile() {
        val scope = baseScope ?: return
        if (disposed) return

        with(scope) { reconcile(items.get()) }
    }

    private fun scheduleClear() {
        val scope = baseScope ?: return
        if (disposed) return

        scope.ui.build.afterBuild {
            if (disposed) return@afterBuild
            val range = ensureRangeCommitted()

            for ((_, im) in mounts) disposeAndRemove(im)
            mounts.clear()
            range.clear()
        }
    }

    private fun disposeAndRemove(im: RangeItemMount) {
        runCatching { im.mount.dispose() }
        runCatching { im.range.dispose() }
    }

    context(scope: NodeScope)
    private fun mountItem(item: T, idx: Int, k: Any, hostRange: RangeInsertPoint): RangeItemMount {
        val itemStart = document.createComment("jFx2:item")
        val itemEnd = document.createComment("jFx2:/item")
        hostRange.insert(itemStart)
        hostRange.insert(itemEnd)

        val itemRange = RangeInsertPoint(itemStart, itemEnd)
        val owner = RangeOwner(itemStart)
        val indexProp = Property(idx)

        val childScope = scope.fork(
            parent = itemRange.parent,
            owner = owner,
            ctx = scope.ctx.fork(),
            insertPoint = itemRange
        )

        // owner renders its children into the item range
        with(childScope) { owner.afterBuild() }

        // ensure markers are removed on disposal
        childScope.dispose.register { itemRange.dispose() }

        val m = componentWithScope(childScope) {
            block(item, indexProp)
        }

        with(childScope) { owner.afterBuild() }

        return RangeItemMount(
            key = k,
            start = itemStart,
            end = itemEnd,
            range = itemRange,
            owner = owner,
            scope = childScope,
            mount = m,
            index = indexProp
        )
    }

    context(scope: NodeScope)
    private fun reconcile(newItems: List<T>) {
        if (disposed) return

        val hostRange = ensureRangeCommitted()
        val parent: Node = start.parentNode ?: return

        val newKeys = ArrayList<Any>(newItems.size)
        for (it in newItems) newKeys += keyOf(it)
        val newKeySet = newKeys.toHashSet()

        run {
            val it = mounts.entries.iterator()
            while (it.hasNext()) {
                val (k, im) = it.next()
                if (k !in newKeySet) {
                    disposeAndRemove(im)
                    it.remove()
                }
            }
        }

        for (i in newItems.indices) {
            val k = newKeys[i]
            if (!mounts.containsKey(k)) {
                mounts[k] = mountItem(newItems[i], i, k, hostRange)
            }
        }

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

        for (i in newItems.indices) {
            mounts.getValue(newKeys[i]).index.set(i)
        }

        if (mounts.size == newKeys.size) {
            val rebuilt = LinkedHashMap<Any, RangeItemMount>(mounts.size * 2)
            for (k in newKeys) rebuilt[k] = mounts.getValue(k)
            mounts.clear()
            mounts.putAll(rebuilt)
        }
    }

    override fun dispose() {
        disposed = true

        for ((_, im) in mounts) runCatching { disposeAndRemove(im) }
        mounts.clear()

        runCatching { committedRange?.dispose() }
        committedRange = null

        runCatching { end.parentNode?.removeChild(end) }

        super.dispose()
    }
}

context(scope: NodeScope)
fun <T> foreach(
    items: ListProperty<T>,
    key: (T) -> Any,
    block: context(NodeScope) (T, Property<Int>) -> Unit
) {
    val start: Comment = document.createComment("jFx2:foreach")
    val end: Comment = document.createComment("jFx2:/foreach")

    val fragment = document.createDocumentFragment()
    fragment.appendChild(start)
    fragment.appendChild(end)

    val comp = ForeachComponent(
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
