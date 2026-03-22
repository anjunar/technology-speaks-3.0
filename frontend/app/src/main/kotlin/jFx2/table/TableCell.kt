package jFx2.table

import jFx2.core.capabilities.NodeScope
import jFx2.core.dom.ElementInsertPoint
import jFx2.state.CompositeDisposable
import jFx2.state.Disposable
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement

class TextCell<R, V>(
    override val node: Element
) : TableCell<R, V>() {

    override fun update(
        rowItem: R?,
        rowIndex: Int,
        value: V?,
        empty: Boolean,
        selected: Boolean,
        focused: Boolean
    ) {
        val el = node as HTMLElement
        el.textContent = if (empty) "" else (value?.toString() ?: "")
        el.classList.toggle("selected", selected)
        el.classList.toggle("focused", focused)
    }

    override fun dispose() {
        // nothing
    }
}

class ComponentCell<R, V>(
    private val outerScope: NodeScope,
    override val node: Element,
    private val render: context(NodeScope) (R, Int, V?) -> Unit
) : RecyclableTableCell<R, V>() {

    private val host = node as HTMLElement

    override fun update(
        rowItem: R?,
        rowIndex: Int,
        value: V?,
        empty: Boolean,
        selected: Boolean,
        focused: Boolean
    ) {
        host.classList.toggle("selected", selected)

        // dispose previous cell subtree and clear host
        resetUpdateBagInternal()
        outerScope.ui.dom.clear(host)

        if (empty || rowItem == null) return

        // render using a scope that targets the host insertPoint
        val cellScope = outerScope.fork(
            parent = host,
            insertPoint = ElementInsertPoint(host)
        )

        // ensure the whole subtree is disposed on recycle
        onUpdateDispose { cellScope.dispose.dispose() }

        with(cellScope) {
            render(rowItem, rowIndex, value)
            ui.build.flush()
        }
    }
}

abstract class RecyclableTableCell<R, V> : TableCell<R, V>() {
    private var updateBag = CompositeDisposable()

    internal fun resetUpdateBagInternal() {
        updateBag.dispose()
        updateBag = CompositeDisposable()
    }

    protected fun onUpdateDispose(d: Disposable) {
        updateBag.add(d)
    }

    override fun dispose() {
        updateBag.dispose()
        onCellDispose()
    }

    protected open fun onCellDispose() {}
}
