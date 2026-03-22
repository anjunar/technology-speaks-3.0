package jFx2.core

import jFx2.core.capabilities.NodeScope
import jFx2.core.dsl.renderFields
import kotlinx.browser.window
import org.w3c.dom.Element

class TemplateOwner(override val node: Element) : Component<Element>() {

    context(scope: NodeScope)
    fun afterBuild() {
        renderFields(*this@TemplateOwner.children.toTypedArray())
    }

}

context(scope: NodeScope)
fun template(block: context(NodeScope) () -> Unit) : TemplateOwner {
    val owner = TemplateOwner(scope.parent.unsafeCast<Element>())
    val s = scope.fork(owner = owner)
    with(s) {
        block()
        owner.afterBuild()
        scope.ui.build.flush()
    }

    return owner
}