package jFx2.virtual

import app.domain.core.AbstractEntity
import app.domain.core.Data
import jFx2.core.Component
import jFx2.core.capabilities.DisposeScope
import jFx2.core.capabilities.NodeScope
import jFx2.core.capabilities.UiScope
import jFx2.core.dom.ElementInsertPoint
import jFx2.state.Disposable
import jFx2.state.ListChange
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.w3c.dom.Element
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.Event
import kotlin.math.max
import kotlin.math.min

private external class ResizeObserver(callback: (entries: Array<dynamic>, observer: ResizeObserver) -> Unit) {
    fun observe(target: Element)
    fun disconnect()
}

/**
 * Virtualized infinite list with:
 * - absolute-positioned cell pool (bounded)
 * - variable row heights (measured)
 * - overscan rendering
 * - prefetch via RangeDataProvider.ensureRange(from, toInclusive)
 *
 * Guardrails:
 * - cells do NOT return Components
 * - each cell render happens in a cell-local NodeScope with its own DisposeScope
 * - dispose on recycle to avoid append-only leaks
 */
class VirtualListView<T : Data<out AbstractEntity>>(
    private val dataProvider: RangeDataProvider<T>,
    private val estimateHeightPx: Int = 44,
    private val overscanPx: Int = 240,
    private val prefetchItems: Int = 80,
    private val renderer: context(NodeScope) (item: T?, index: Int) -> Unit
) : Component<HTMLDivElement>() {

    override val node: HTMLDivElement = document.createElement("div") as HTMLDivElement

    private lateinit var uiScope: UiScope
    private lateinit var baseScope: NodeScope
    private lateinit var viewport: HTMLDivElement
    private lateinit var content: HTMLDivElement

    private val job = SupervisorJob()
    private val cs = CoroutineScope(job)

    private var resizeObserver: ResizeObserver? = null

    private var loadingJob: Job? = null
    private var pendingTargetTo = -1
    private var pendingTargetFrom = -1

    /**
     * When count is unknown and end not reached we add tail padding (in *items*)
     * to allow scrolling further while data loads.
     */
    private var tailPaddingItems = prefetchItems * 3

    /**
     * Heights known for loaded indices. For indices beyond, estimateHeightPx is used.
     *
     * prefix[i] = sum(heights[0..i-1])  => prefix size == heights.size + 1, prefix[0] = 0
     */
    private val heights = ArrayList<Int>()
    private val prefix = ArrayList<Int>().apply { add(0) }

    // Lazy prefix recomputation
    private var prefixDirtyFrom: Int = Int.MAX_VALUE

    private data class Slot(
        val node: HTMLDivElement,
        var boundIndex: Int = -1,
        var loaded: Boolean = false,
        var dispose: DisposeScope? = null
    )

    private val slots = ArrayList<Slot>()

    private var renderScheduled = false
    private var measureScheduled = false
    private var disposed = false

    init {
        node.className = "jfx-virtual-list"
        onDispose(Disposable { job.cancel() })
        onDispose(Disposable { slots.forEach { it.dispose?.dispose() } })
    }

    context(scope: NodeScope)
    fun build(): VirtualListView<T> {
        uiScope = scope.ui
        baseScope = scope
        uiScope.dom.clear(node)

        resizeObserver = runCatching {
            val supported = (js("typeof ResizeObserver !== 'undefined'") as Boolean)
            if (!supported) null else ResizeObserver { _, _ -> scheduleMeasure() }
        }.getOrNull()
        resizeObserver?.let { ro ->
            onDispose(Disposable { ro.disconnect() })
        }

        viewport = scope.create<HTMLDivElement>("div").apply {
            className = "jfx-virtual-list-viewport"
            tabIndex = 0
        }

        content = scope.create<HTMLDivElement>("div").apply {
            className = "jfx-virtual-list-content"
        }

        viewport.appendChild(content)
        node.appendChild(viewport)

        viewport.addEventListener("scroll", onScroll)
        onDispose(Disposable { viewport.removeEventListener("scroll", onScroll) })

        val resize = windowResizeListener()
        onDispose(resize)

        dataProvider.let { observable ->
            val d = observable.observeChanges(::onProviderChange)
            onDispose(d)
        }

        scheduleRender()
        // Initial prefetch
        requestRange(from = 0, toInclusiveRaw = prefetchItems)

        return this
    }

    override fun dispose() {
        disposed = true
        super.dispose()
        loadingJob = null
    }

    private val onScroll: (Event) -> Unit = { scheduleRender() }

    private fun windowResizeListener(): Disposable {
        val listener: (Event) -> Unit = { scheduleRender() }
        window.addEventListener("resize", listener)
        return Disposable { window.removeEventListener("resize", listener) }
    }

    private fun scheduleRender() {
        if (disposed) return
        if (renderScheduled) return
        renderScheduled = true
        window.requestAnimationFrame {
            renderScheduled = false
            render()
        }
    }

    private fun scheduleMeasure() {
        if (disposed) return
        if (measureScheduled) return
        measureScheduled = true
        window.requestAnimationFrame {
            measureScheduled = false
            if (disposed) return@requestAnimationFrame

            var changed = false
            slots.forEach { slot ->
                val idx = slot.boundIndex
                if (idx >= 0 && slot.loaded) {
                    val h = slot.node.offsetHeight
                    if (h > 0 && updateHeight(idx, h)) {
                        changed = true
                    }
                }
            }

            if (changed) {
                rebuildPrefixIfDirty()
                updateContentHeight()
                // re-render because offsets may have shifted due to new measured heights
                render()
            }
        }
    }

    private fun ensureHeightsSize(size: Int) {
        if (size <= 0) return
        while (heights.size < size) {
            heights.add(estimateHeightPx)
            // maintain prefix invariant: prefix size == heights size + 1
            prefix.add(prefix.last() + estimateHeightPx)
        }
    }

    private fun resetMeasurements() {
        heights.clear()
        prefix.clear()
        prefix.add(0)
        prefixDirtyFrom = Int.MAX_VALUE
    }

    private fun invalidateSlots() {
        slots.forEach { slot ->
            slot.boundIndex = -1
            slot.loaded = false
        }
    }

    private fun onProviderChange(change: ListChange<*>) {
        if (disposed) return

        // Keep measurements on append-only changes; reset when indices can shift.
        val reset = when (change) {
            is ListChange.Add -> change.fromIndex < heights.size
            is ListChange.Remove -> true
            is ListChange.Replace -> false
            is ListChange.Clear -> true
            is ListChange.SetAll -> true
        }

        if (reset) resetMeasurements()

        // Ensure re-render when an existing index may now point to a different item.
        if (reset || change is ListChange.Replace) invalidateSlots()

        scheduleRender()
    }

    /**
     * Mark prefix dirty; we rebuild lazily in a batch (one pass) instead of O(n) per changed row.
     */
    private fun updateHeight(index: Int, newHeight: Int): Boolean {
        if (index < 0) return false
        ensureHeightsSize(index + 1)
        val old = heights[index]
        if (old == newHeight) return false
        heights[index] = newHeight
        prefixDirtyFrom = min(prefixDirtyFrom, index + 1) // prefix[i] depends on heights[i-1]
        return true
    }

    private fun rebuildPrefixIfDirty() {
        val from = prefixDirtyFrom
        if (from == Int.MAX_VALUE) return
        // from is in [1..prefix.lastIndex]
        val start = max(1, min(from, prefix.lastIndex))
        for (i in start..prefix.lastIndex) {
            prefix[i] = prefix[i - 1] + heights[i - 1]
        }
        prefixDirtyFrom = Int.MAX_VALUE
    }

    private fun offsetFor(index: Int): Int {
        val loaded = heights.size
        return if (index <= loaded) {
            // prefix has size loaded+1, so prefix[index] is valid for index<=loaded
            prefix[index]
        } else {
            prefix.last() + (index - loaded) * estimateHeightPx
        }
    }

    private fun indexForOffset(offset: Int): Int {
        val off = max(0, offset)
        val loaded = heights.size
        if (loaded == 0) return 0

        val totalKnownHeight = prefix.last()
        if (off >= totalKnownHeight) {
            return loaded + ((off - totalKnownHeight) / estimateHeightPx)
        }

        // binary search in prefix: find largest i where prefix[i] <= off, then index = i
        var lo = 0
        var hi = loaded // prefix indices [0..loaded]
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (prefix[mid + 1] <= off) lo = mid + 1 else hi = mid
        }
        return lo
    }

    private fun heightFor(index: Int): Int =
        if (index in heights.indices) heights[index] else estimateHeightPx

    private fun updateContentHeight() {
        val knownCount = if (dataProvider.hasKnownCount) dataProvider.knownCount else null
        val base = prefix.last()

        val extra = when {
            dataProvider.endReached -> 0
            knownCount != null -> max(0, knownCount - heights.size) * estimateHeightPx
            else -> tailPaddingItems * estimateHeightPx
        }

        content.style.height = "${base + extra}px"
    }

    /**
     * Hard cap the slot pool to avoid DOM ballooning.
     */
    private fun maxSlotsForViewport(viewportH: Int): Int {
        // a conservative minimum row height to bound the pool
        val minRowH = max(12, min(estimateHeightPx, estimateHeightPx / 2))
        val area = viewportH + 2 * overscanPx
        val raw = (area / minRowH) + 8 // + safety
        // keep a reasonable upper bound; adjust if your use-case truly needs more
        return min(600, max(32, raw))
    }

    private fun ensureSlotCount(needed: Int) {
        val missing = needed - slots.size
        if (missing <= 0) return
        repeat(missing) {
            val n = baseScope.create<HTMLDivElement>("div").apply {
                className = "jfx-virtual-list-cell"
            }
            content.appendChild(n)
            resizeObserver?.observe(n)
            slots.add(Slot(n))
        }
    }

    private fun renderSlot(slot: Slot, item: T?, index: Int) {
        slot.dispose?.dispose()
        uiScope.dom.clear(slot.node)

        val dispose = DisposeScope()
        slot.dispose = dispose

        val slotScope = NodeScope(
            ui = uiScope,
            parent = slot.node,
            owner = this,               // cell subtree owned by this component
            ctx = baseScope.ctx,
            dispose = dispose,
            insertPoint = ElementInsertPoint(slot.node)
        )

        with(slotScope) {
            renderer(item, index)
        }
    }

    /**
     * Requests the given range. Supports "real" ranges (not only 0..N).
     * We still keep a simple pending mechanism to avoid overlapping loads.
     */
    private fun requestRange(from: Int, toInclusiveRaw: Int) {
        if (disposed) return
        if (dataProvider.endReached) return

        val knownCount = if (dataProvider.hasKnownCount) dataProvider.knownCount else null
        val toInclusive = if (knownCount != null) min(toInclusiveRaw, knownCount - 1) else toInclusiveRaw
        if (toInclusive < 0) return
        if (from > toInclusive) return

        // if already loaded enough (best-effort, depends on provider semantics)
        // We only skip if the requested "to" is inside loadedCount and provider is append-ish.
        if (toInclusive < dataProvider.loadedCount && from <= dataProvider.loadedCount - 1) return

        if (loadingJob?.isActive == true) {
            pendingTargetFrom = if (pendingTargetFrom < 0) from else min(pendingTargetFrom, from)
            pendingTargetTo = max(pendingTargetTo, toInclusive)
            return
        }

        loadingJob = cs.launch {
            dataProvider.ensureRange(from, toInclusive)
            ensureHeightsSize(dataProvider.loadedCount)

            if (dataProvider.endReached) tailPaddingItems = 0
            rebuildPrefixIfDirty()
            updateContentHeight()
            scheduleRender()

            val pf = pendingTargetFrom
            val pt = pendingTargetTo
            pendingTargetFrom = -1
            pendingTargetTo = -1
            if (pf >= 0 && pt >= 0) requestRange(pf, pt)
        }
    }

    private fun render() {
        if (disposed) return
        val viewportH = viewport.clientHeight
        if (viewportH <= 0) return

        ensureHeightsSize(dataProvider.loadedCount)
        rebuildPrefixIfDirty()
        updateContentHeight()

        val scrollTop = viewport.scrollTop.toInt()
        val startOffset = max(0, scrollTop - overscanPx)
        val endOffset = scrollTop + viewportH + overscanPx

        val knownCount = if (dataProvider.hasKnownCount) dataProvider.knownCount else null
        val maxCount = when {
            knownCount != null -> knownCount
            dataProvider.endReached -> dataProvider.loadedCount
            else -> Int.MAX_VALUE
        }

        val maxSlots = maxSlotsForViewport(viewportH)

        var index = indexForOffset(startOffset)
        var top = offsetFor(index)

        val visible = ArrayList<Pair<Int, Int>>(min(256, maxSlots))

        while (top < endOffset && index < maxCount && visible.size < maxSlots) {
            visible.add(index to top)
            top += heightFor(index)
            index += 1
        }

        if (visible.isEmpty()) {
            // IMPORTANT: cleanup previously rendered slots (e.g. stale "Loading..." placeholders)
            for (i in 0 until slots.size) {
                val slot = slots[i]
                slot.node.classList.add("is-hidden")
                slot.boundIndex = -1
                slot.loaded = false
                // optional: clear content so it doesn't flash if shown later
                uiScope.dom.clear(slot.node)
                slot.dispose?.dispose()
                slot.dispose = null
            }
            // keep height consistent
            updateContentHeight()
            return
        }

        val needed = visible.size
        ensureSlotCount(needed)

        var anyRendered = false

        // Update visible slots
        for (i in 0 until needed) {
            val slot = slots[i]
            val (idx, topPx) = visible[i]

            slot.node.style.top = "${topPx}px"
            slot.node.classList.remove("is-hidden")

            val item = dataProvider.getOrNull(idx)
            val loaded = item != null

            if (slot.boundIndex != idx || slot.loaded != loaded) {
                renderSlot(slot, item, idx)
                anyRendered = true
            }

            slot.boundIndex = idx
            slot.loaded = loaded
        }

        // Hide unused slots (keep pool bounded by maxSlots anyway)
        for (i in needed until slots.size) {
            val slot = slots[i]
            slot.node.classList.add("is-hidden")
            slot.boundIndex = -1
            slot.loaded = false
        }

        if (anyRendered) {
            // Flush once per frame, not per slot
            uiScope.build.flush()
        }

        // Prefetch logic based on current window, not always 0..N.
        if (!dataProvider.endReached && maxCount > 0) {
            val startIndex = visible.first().first
            val endIndex = visible.last().first

            val from = max(0, startIndex - prefetchItems)
            val to = min(endIndex + prefetchItems, maxCount - 1)

            // For unknown count, extend tail padding if we scroll near its end.
            if (knownCount == null && !dataProvider.endReached) {
                val projectedEnd = heights.size + tailPaddingItems - 1
                if (to > projectedEnd) {
                    tailPaddingItems += prefetchItems * 2
                    updateContentHeight()
                }
            }

            requestRange(from, to)
        }

        scheduleMeasure()
    }
}
