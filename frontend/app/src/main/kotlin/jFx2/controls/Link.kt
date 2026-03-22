package jFx2.controls

import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dom.ElementInsertPoint
import jFx2.core.dsl.renderFields
import kotlinx.browser.window
import org.w3c.dom.CustomEvent
import org.w3c.dom.HTMLAnchorElement

@JfxComponentBuilder
class Link(val href : String, override val node: HTMLAnchorElement) : Component<HTMLAnchorElement>() {

    init {
        node.href = href
        node.addEventListener("click", {
            it.preventDefault()
            window.history.pushState(null, "", href)
            window.dispatchEvent(CustomEvent("popstate"))
        })
    }

    context(scope: NodeScope)
    fun afterBuild() {
        renderFields(*this@Link.children.toTypedArray())
    }

}