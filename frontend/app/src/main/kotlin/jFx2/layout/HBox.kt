package jFx2.layout

import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.capabilities.UiScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dom.ElementInsertPoint
import jFx2.core.dsl.renderFields
import org.w3c.dom.HTMLDivElement

@JfxComponentBuilder(name = "hbox", classes = ["hbox"])
class HBox(override val node: HTMLDivElement) : Component<HTMLDivElement>() {

    context(scope: NodeScope)
    fun afterBuild() {
        renderFields(*this@HBox.children.toTypedArray())
    }


}