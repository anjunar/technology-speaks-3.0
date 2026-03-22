package jFx2.table

import jFx2.state.CompositeDisposable
import jFx2.state.Property
import jFx2.state.Disposable
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

internal class VirtualTableFlow<R>(
    private val viewport: HTMLElement,
    private val content: HTMLElement,
    private val rowHeightPx: Int,
    private val overscan: Int,
    private val columns: List<Column<R, *>>,
    private val model: LazyTableModel<R>,
    private val selection: SelectionModel,
    private val focus: FocusModel
) : Disposable {

    class CellHolder<R, V>(
        val col: Column<R, V>,
        val cell: TableCell<R, V>,
        val host: HTMLElement
    ) : Disposable {

        private val bindings = CompositeDisposable()

        private var boundIndex: Int = Int.MIN_VALUE
        private var boundItem: Any? = null
        private var currentValue: V? = null
        private var isEmpty: Boolean = true

        fun bindAndUpdate(
            rowItem: R?,
            rowIndex: Int,
            empty: Boolean,
            selected: Boolean,
            focused: Boolean
        ) {
            if (boundIndex == rowIndex && boundItem === rowItem && isEmpty == empty) {
                cell.update(rowItem, rowIndex, currentValue, empty, selected, focused)
                return
            }

            bindings.dispose()

            boundIndex = rowIndex
            boundItem = rowItem
            isEmpty = empty
            currentValue = null

            if (empty || rowItem == null) {
                cell.update(null, rowIndex, null, true, selected, focused)
                return
            }

            val vp = col.valueProperty
            if (vp != null) {
                val prop = vp(rowItem)
                if (prop != null) {
                    val d = prop.observe { v ->
                        currentValue = v
                        cell.update(rowItem, rowIndex, v, false, selected, focused)
                    }
                    bindings.add(d)
                }
            } else {
                @Suppress("UNCHECKED_CAST")
                val vf = col.value as (R) -> V
                val v = vf(rowItem)
                currentValue = v
                cell.update(rowItem, rowIndex, v, false, selected, focused)
            }
        }

        override fun dispose() {
            bindings.dispose()
            cell.dispose()
        }
    }

    class VirtualRow<R>(
        val node: HTMLElement,
        val cells: List<CellHolder<R, *>>
    ) : Disposable {
        var boundIndex: Int = -1
        fun setTopPx(px: Int) { node.style.top = "${px}px" }
        override fun dispose() { cells.forEach { it.dispose() } }
    }

    private val rows = ArrayList<VirtualRow<R>>()

    private val scrollListener: (Event) -> Unit = { render() }

    private val dInvalidate = model.invalidateTick.observe { render() }
    private val dCount = model.totalCount.observe { updateContentHeight(); render() }
    private val dLoaded = model.hasLoadedOnce.observe { updateContentHeight(); render() }
    private val dSel = selection.selected.observe { render() }
    private val dFocus = focus.focusedIndex.observe { render() }

    init {
        viewport.addEventListener("scroll", scrollListener)
        updateContentHeight()
    }

    fun scrollIntoView(index: Int) {
        if (index < 0) return
        val top : Double = index.toDouble() * rowHeightPx
        val bottom = top + rowHeightPx

        val viewTop = viewport.scrollTop
        val viewBottom = viewTop + viewport.clientHeight

        when {
            top < viewTop -> viewport.scrollTop = top
            bottom > viewBottom -> viewport.scrollTop = bottom - viewport.clientHeight
        }
    }

    override fun dispose() {
        viewport.removeEventListener("scroll", scrollListener)
        dInvalidate.dispose()
        dCount.dispose()
        dLoaded.dispose()
        dSel.dispose()
        dFocus.dispose()
        rows.forEach { it.dispose() }
        rows.clear()
    }

    fun setRows(pool: List<VirtualRow<R>>) {
        rows.clear()
        rows.addAll(pool)
        render()
    }

    private fun updateContentHeight() {
        val total = model.totalCount.get()
        val effectiveTotal = if (total == 0 && !model.hasLoadedOnce.get()) null else total
        content.style.height =
            if (effectiveTotal != null) "${effectiveTotal * rowHeightPx}px" else "${1000 * rowHeightPx}px"
    }

    fun render() {
        if (rows.isEmpty()) return

        val viewportH = viewport.clientHeight
        val scrollTop = viewport.scrollTop

        val totalKnownRaw = model.totalCount.get()
        val totalKnown = if (totalKnownRaw == 0 && !model.hasLoadedOnce.get()) null else totalKnownRaw
        val first = max(0, floor(scrollTop / rowHeightPx).toInt() - overscan)
        val visible = ceil(viewportH.toDouble() / rowHeightPx).toInt() + overscan * 2
        val lastExclusive = first + visible

        val start: Int
        val endExclusive: Int
        if (totalKnown != null) {
            start = min(first, max(0, totalKnown - 1))
            endExclusive = min(lastExclusive, totalKnown)
        } else {
            start = first
            endExclusive = lastExclusive
            val requiredHeight = endExclusive * rowHeightPx
            if (requiredHeight > content.offsetHeight) {
                content.style.height = "${requiredHeight + (rowHeightPx * 500)}px"
            }
        }

        val count = max(0, endExclusive - start)
        if (count == 0) {
            for (row in rows) {
                row.node.classList.add("is-hidden")
                row.boundIndex = -1
            }
            return
        }

        val used = min(count, rows.size)

        for (slot in 0 until used) {
            val index = start + slot
            val row = rows[slot]
            row.boundIndex = index
            row.setTopPx(index * rowHeightPx)

            val item = model.get(index)
            val empty = item == null

            val selected = selection.isSelected(index)
            val focused = focus.focusedIndex.get() == index

            row.node.classList.toggle("selected", selected)
            row.node.classList.remove("is-hidden")

            row.cells.forEach { holderAny ->
                @Suppress("UNCHECKED_CAST")
                val holder = holderAny as CellHolder<R, Any?>
                holder.bindAndUpdate(item, index, empty, selected, focused)
            }
        }

        for (i in used until rows.size) {
            rows[i].node.classList.add("is-hidden")
            rows[i].boundIndex = -1
        }
    }

    companion object {
        internal fun <R, V> cellHolder(col: Column<R, V>, cell: TableCell<R, V>, host: HTMLElement) =
            CellHolder(col, cell, host)

        internal fun <R> virtualRow(node: HTMLElement, cells: List<CellHolder<R, *>>) =
            VirtualRow(node, cells)
    }
}
