package jFx2.table

import jFx2.core.capabilities.NodeScope
import jFx2.state.ReadOnlyProperty

class TableBuilder<R> {
    internal val cols = ArrayList<Column<R, *>>()
    internal var onSelectionChanged: ((List<R>) -> Unit)? = null
    internal var onRowDoubleClick: ((R, Int) -> Unit)? = null
    internal var onRowClick: ((R, Int) -> Unit)? = null

    fun <V> columnValue(
        id : String,
        header: String,
        prefWidthPx: Int = 160,
        value: (R) -> V,
        cellFactory: context(NodeScope) () -> TableCell<R, V>
    ) {
        cols += Column(
            id = id,
            header = header,
            prefWidthPx = prefWidthPx,
            value = value,
            valueProperty = null,
            cellFactory = cellFactory
        )
    }

    fun <V> columnProperty(
        id : String,
        header: String,
        prefWidthPx: Int = 160,
        value: ((R) -> V)? = null,
        valueProperty: ((R) -> ReadOnlyProperty<V>?)? = null,
        cellFactory: context(NodeScope) () -> TableCell<R, V>
    ) {
        cols += Column(
            id = id,
            header = header,
            prefWidthPx = prefWidthPx,
            value = value,
            valueProperty = valueProperty,
            cellFactory = cellFactory
        )
    }

    fun onSelectionChanged(handler: (List<R>) -> Unit) {
        onSelectionChanged = handler
    }

    fun onRowDoubleClick(handler: (R, Int) -> Unit) {
        onRowDoubleClick = handler
    }

    fun onRowClick(handler: (R, Int) -> Unit) {
        onRowClick = handler
    }
}

context(scope: NodeScope)
fun <R> tableView(
    model: LazyTableModel<R>,
    rowHeightPx: Int = 28,
    overscan: Int = 6,
    headerVisible: Boolean = true,
    block: TableBuilder<R>.() -> Unit
): TableView<R> {
    val b = TableBuilder<R>().apply(block)
    val tableView = TableView(
        model = model,
        columns = b.cols,
        rowHeightPx = rowHeightPx,
        overscan = overscan,
        headerVisible = headerVisible
    )

    tableView.onSelectionChanged = b.onSelectionChanged
    tableView.onRowDoubleClick = b.onRowDoubleClick
    tableView.onRowClick = b.onRowClick

    scope.attach(tableView)

    return tableView.build()
}
