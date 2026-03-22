package jFx2.controls

import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.capabilities.UiScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dom.ElementInsertPoint
import jFx2.core.dsl.renderFields
import jFx2.state.Disposable
import jFx2.state.Property
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.events.MouseEvent

@JfxComponentBuilder
class Button(val name : String, override val node: HTMLButtonElement) : Component<HTMLButtonElement>() {

    init {
        node.textContent = name
    }

    context(scope: NodeScope)
    fun afterBuild() {
        renderFields(*this@Button.children.toTypedArray())
    }

    fun name(name : String) {
        node.textContent = name
    }

    fun disabled(value: Boolean) {
        node.disabled = value
    }

    fun disabled(flag: Property<Boolean>) {
        onDispose(flag.observe { node.disabled = it })
    }

    fun type(value: String) {
        node.type = value
    }

    fun text(value: String) {
        node.textContent = value
    }

    fun text(value: () -> String) {
        node.textContent = value()
    }
}