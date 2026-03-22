package jFx2.controls

import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.capabilities.UiScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dom.ElementInsertPoint
import jFx2.core.dsl.renderFields
import org.w3c.dom.HTMLSpanElement

@JfxComponentBuilder
class Span(override val node: HTMLSpanElement, val ui: UiScope) : Component<HTMLSpanElement>() {

    context(scope: NodeScope)
    fun afterBuild() {
        renderFields(*this@Span.children.toTypedArray())
    }

}