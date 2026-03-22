package jFx2.table

import jFx2.core.capabilities.NodeScope
import jFx2.state.Disposable
import jFx2.state.Property
import jFx2.state.ReadOnlyProperty
import org.w3c.dom.Element

class Column<R, V>(
    val id: String,
    val header: String,
    val prefWidthPx: Int = 160,

    val value: ((R) -> V)? = null,
    val valueProperty: ((R) -> ReadOnlyProperty<V>?)? = null,

    val sortable: Boolean = true,
    val cellFactory: context(NodeScope) () -> TableCell<R, V>
) {
    val width = Property(prefWidthPx)

    init {
        require(value != null || valueProperty != null) {
            "Column requires either value or valueProperty"
        }
        require(!(value != null && valueProperty != null)) {
            "Column requires only one of value or valueProperty"
        }
    }
}

abstract class TableCell<R, V> : Disposable {
    abstract val node: Element

    abstract fun update(
        rowItem: R?,
        rowIndex: Int,
        value: V?,
        empty: Boolean,
        selected: Boolean,
        focused: Boolean
    )
}
