package jFx2.core.capabilities

import jFx2.core.Component
import jFx2.core.Ctx
import jFx2.core.dom.DomInsertPoint
import jFx2.core.dom.ElementInsertPoint
import jFx2.state.Disposable
import org.w3c.dom.Element
import org.w3c.dom.Node
import kotlinx.browser.document

class DomScope {
    fun <E : Element> create(tag: String): E = document.createElement(tag).unsafeCast<E>()
    fun attach(parent: Node, child: Node) { parent.appendChild(child) }
    fun detach(node: Node) { node.parentNode?.removeChild(node) }
    fun clear(node: Node) { while (node.firstChild != null) node.removeChild(node.firstChild!!) }
}

class BuildScope {
    private val after = ArrayList<() -> Unit>()
    private val dirty = ArrayList<() -> Unit>()

    fun afterBuild(fn: () -> Unit) { after.add(fn) }

    fun dirty(fn: () -> Unit) { dirty.add(fn) }

    fun flush() {
        while (dirty.isNotEmpty()) {
            val fn = dirty.removeAt(0)
            fn()
        }

        if (after.isEmpty()) return
        val copy = after.toList()
        after.clear()
        for (f in copy) f()
    }
}

class UiScope(
    val dom: DomScope = DomScope(),
    val build: BuildScope = BuildScope(),
    val dispose: DisposeScope = DisposeScope()
)

class NodeScope(
    val ui: UiScope,
    val parent: Node,
    val owner: Component<*>,
    val ctx: Ctx,
    val dispose: DisposeScope,
    val insertPoint: DomInsertPoint = ElementInsertPoint(parent)
) {
    fun <E : Element> create(tag: String): E = ui.dom.create(tag)

    fun attach(child: Component<*>) {
        owner.addChild(child)

        dispose.register {
            runCatching { child.dispose() }
            owner.removeChild(child)
        }
    }

    fun fork(
        parent: Node = this.parent,
        owner: Component<*> = this.owner,
        ctx: Ctx = this.ctx,
        insertPoint: DomInsertPoint = this.insertPoint
    ): NodeScope {
        val childDispose = DisposeScope()
        dispose.register { childDispose.dispose() }

        return NodeScope(
            ui = ui,
            parent = parent,
            owner = owner,
            ctx = ctx,
            dispose = childDispose,
            insertPoint = insertPoint
        )
    }
}