package jFx2.forms

import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.AfterBuildMode
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dom.ElementInsertPoint
import org.w3c.dom.HTMLOptionElement

@JfxComponentBuilder
class Option(override val node: HTMLOptionElement) : Component<HTMLOptionElement>() {

    fun value(value: String) {
        node.value = value
    }

}