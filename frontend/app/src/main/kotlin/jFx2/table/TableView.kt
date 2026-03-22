package jFx2.table

import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.on
import jFx2.state.Disposable
import jFx2.state.Property
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import kotlin.js.unsafeCast

class TableView<R>(
    private val model: LazyTableModel<R>,
    private val columns: List<Column<R, *>>,
    private val headerVisible : Boolean = true,
    private val rowHeightPx: Int = 28,
    private val overscan: Int = 6
) : Component<HTMLDivElement>() {

    override val node: HTMLDivElement = document.createElement("div") as HTMLDivElement

    val sortState = Property<SortState?>(null)
    var onSortRequested: ((SortState?) -> Unit)? = null

    val selectionModel = SelectionModel(SelectionMode.MULTIPLE)
    val focusModel = FocusModel()

    var onSelectionChanged: ((List<R>) -> Unit)? = null
    var onRowDoubleClick: ((R, Int) -> Unit)? = null
    var onRowClick: ((R, Int) -> Unit)? = null

    private lateinit var viewport: HTMLElement
    private lateinit var content: HTMLElement

    private var keyListener: ((Event) -> Unit)? = null

    private var flow: VirtualTableFlow<R>? = null

    init {
        node.className = "jfx-table"
    }

    context(scope: NodeScope)
    fun build(): TableView<R> {
        scope.ui.dom.clear(node)

        node.style.setProperty("--row-height", "${rowHeightPx}px")

        if (headerVisible) {
            val header = scope.create<HTMLDivElement>("div").apply {
                className = "jfx-table-header"
            }

            onDispose(sortState.observeWithoutInitial { s ->
                model.clearCache()
            })

            columns.forEach { col ->
                val headerCell = scope.create<HTMLDivElement>("div").apply {
                    className = "jfx-table-header-cell"
                }

                onDispose(col.width.observe { w ->
                    headerCell.style.width = "${w}px"
                    headerCell.style.minWidth = "${w}px"
                    headerCell.style.maxWidth = "${w}px"
                })

                val title = scope.create<HTMLDivElement>("div").apply {
                    className = "jfx-table-header-title"
                }

                val sortIndicator = scope.create<HTMLDivElement>("div").apply {
                    className = "jfx-table-sort-indicator"
                }

                onDispose(sortState.observe { s ->
                    sortIndicator.textContent =
                        if (s?.columnId == col.id) (if (s.direction == SortDirection.ASC) "▲" else "▼")
                        else ""
                })

                title.textContent = col.header
                headerCell.appendChild(title)
                headerCell.appendChild(sortIndicator)

                onDispose(col.width.observe { w ->
                    headerCell.style.width = "${w}px"
                })

                if (col.sortable) {
                    headerCell.addEventListener("click", { ev ->
                        val current = sortState.get()
                        val next =
                            if (current?.columnId != col.id) {
                                SortState(col.id, SortDirection.ASC)
                            } else {
                                when (current.direction) {
                                    SortDirection.ASC -> SortState(col.id, SortDirection.DESC)
                                    SortDirection.DESC -> null
                                }
                            }

                        sortState.set(next)
                        onSortRequested?.invoke(next)
                    })
                }

                val handle = scope.create<HTMLDivElement>("div").apply {
                    className = "jfx-table-resize-handle"
                }

                handle.addEventListener("mousedown", { e ->
                    val me = e.unsafeCast<MouseEvent>()
                    me.preventDefault()
                    me.stopPropagation()

                    val startX = me.clientX
                    val startW = col.width.get()

                    lateinit var moveD: Disposable
                    lateinit var upD: Disposable

                    moveD = window.on("mousemove") { ev ->
                        val mm = ev.unsafeCast<MouseEvent>()
                        val dx = mm.clientX - startX
                        val newW = kotlin.math.max(40, startW + dx)
                        col.width.set(newW)
                    }
                    upD = window.on("mouseup") { _ ->
                        moveD.dispose()
                        upD.dispose()
                    }
                })

                headerCell.appendChild(handle)
                header.appendChild(headerCell)
            }

            node.appendChild(header)
        }


        viewport = scope.create<HTMLDivElement>("div").apply {
            className = "jfx-table-viewport"
        }

        content = scope.create<HTMLDivElement>("div").apply {
            className = "jfx-table-content"
        }

        fun updateContentWidth() {
            var total = 0
            for (c in columns) total += c.width.get()
            if (total <= 0) total = 1
            content.style.width = "${total}px"
        }

        // When the header is hidden, rows are absolutely positioned and do not contribute to intrinsic width.
        // Keeping the content width in sync with column widths prevents the table from collapsing to 0px.
        columns.forEach { col ->
            onDispose(col.width.observe { _ -> updateContentWidth() })
        }
        updateContentWidth()

        viewport.appendChild(content)
        node.appendChild(viewport)

        onDispose(selectionModel.selected.observe { indices ->
            val items = indices.mapNotNull { idx -> model.get(idx) }
            onSelectionChanged?.invoke(items)
        })

        val poolSize = computeInitialPoolSize(viewport, rowHeightPx, overscan)
        val pool = buildRowPool(poolSize)

        val f = VirtualTableFlow(
            viewport = viewport,
            content = content,
            rowHeightPx = rowHeightPx,
            overscan = overscan,
            columns = columns,
            model = model,
            selection = selectionModel,
            focus = focusModel,
        )
        f.setRows(pool)

        flow = f

        val listener: (Event) -> Unit = { e ->
            val ke = e.unsafeCast<KeyboardEvent>()

            val totalRaw = model.totalCount.get()
            val total = if (totalRaw == 0 && !model.hasLoadedOnce.get()) null else totalRaw
            val page = kotlin.math.max(1, viewport.clientHeight / rowHeightPx)

            fun clamp(i: Int): Int {
                if (i < 0) return 0
                if (total != null) {
                    if (total <= 0) return 0
                    return kotlin.math.min(i, total - 1)
                }
                return i // unknown => allow forward
            }

            val current =
                focusModel.focusedIndex.get()
                    ?: selectionModel.selectedIndex.get()
                    ?: 0

            val additive = ke.ctrlKey || ke.metaKey
            val range = ke.shiftKey

            fun moveTo(targetRaw: Int) {
                val target = clamp(targetRaw)
                focusModel.focus(target)
                selectionModel.select(target, additive = additive, range = range)
                flow?.scrollIntoView(target)
                ke.preventDefault()
                ke.stopPropagation()
            }

            when (ke.key) {
                "ArrowUp" -> moveTo(current - 1)
                "ArrowDown" -> moveTo(current + 1)
                "PageUp" -> moveTo(current - page)
                "PageDown" -> moveTo(current + page)
                "Home" -> moveTo(0)
                "End" -> {
                    if (total != null && total > 0) moveTo(total - 1)
                    else {
                        // unknown: wir können nur "weiter nach unten" nicht sinnvoll springen
                        // -> mach nix oder spring z.B. current+page*10
                        moveTo(current + page * 10)
                    }
                }
                " " /* Space */ , "Enter" -> {
                    moveTo(current)
                }
                else -> Unit
            }
        }

        keyListener = listener
        viewport.addEventListener("keydown", listener)

        onDispose {
            val l = keyListener
            if (l != null) viewport.removeEventListener("keydown", l)
        }

        onDispose(f)

        return this
    }

    override fun dispose() {
        super.dispose()
        flow = null
    }

    private fun computeInitialPoolSize(viewport: HTMLElement, rowHeightPx: Int, overscan: Int): Int {
        val vh = viewport.clientHeight.takeIf { it > 0 } ?: 600
        val visible = kotlin.math.ceil(vh.toDouble() / rowHeightPx).toInt()
        return visible + overscan * 2
    }

    context(scope: NodeScope)
    private fun buildRowPool(poolSize: Int): List<VirtualTableFlow.VirtualRow<R>> {
        val rows = ArrayList<VirtualTableFlow.VirtualRow<R>>(poolSize)

        repeat(poolSize) { _ ->
            val rowEl = scope.create<HTMLDivElement>("div").apply {
                className = "jfx-table-row"
            }

            val cellHolders = columns.map { col ->
                val host = scope.create<HTMLDivElement>("div").apply {
                    className = "jfx-table-cell-host"
                }

                onDispose(col.width.observe { w ->
                    host.style.width = "${w}px"
                    host.style.minWidth = "${w}px"
                    host.style.maxWidth = "${w}px"
                })

                rowEl.appendChild(host)

                @Suppress("UNCHECKED_CAST")
                val typedCol = col as Column<R, Any?>

                val cell = typedCol.cellFactory(scope)

                host.appendChild(cell.node)

                VirtualTableFlow.cellHolder(typedCol, cell, host)
            }

            rowEl.addEventListener("click", { ev ->
                val idx = rows.firstOrNull { it.node === rowEl }?.boundIndex ?: -1
                if (idx >= 0) {
                    val me = ev.unsafeCast<MouseEvent>()
                    val additive = me.ctrlKey || me.metaKey
                    val range = me.shiftKey

                    focusModel.focus(idx)
                    selectionModel.select(idx, additive = additive, range = range)
                    viewport.focus()
                }
            })

            rowEl.addEventListener("dblclick", {
                val idx = rows.firstOrNull { it.node === rowEl }?.boundIndex ?: -1
                if (idx >= 0) {
                    val item = model.get(idx)
                    if (item != null) {
                        onRowDoubleClick?.invoke(item, idx)
                    }
                }
            })

            rowEl.addEventListener("click", {
                val idx = rows.firstOrNull { it.node === rowEl }?.boundIndex ?: -1
                if (idx >= 0) {
                    val item = model.get(idx)
                    if (item != null) {
                        onRowClick?.invoke(item, idx)
                    }
                }
            })

            viewport.addEventListener("focus", {
                if (focusModel.focusedIndex.get() == null) {
                    val idx = selectionModel.selectedIndex.get() ?: 0
                    focusModel.focus(idx)
                    selectionModel.select(idx)
                    flow?.scrollIntoView(idx)
                }
            })

            content.appendChild(rowEl)
            rows += VirtualTableFlow.virtualRow(rowEl, cellHolders)
        }

        return rows
    }
}
