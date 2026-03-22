package jFx2.core.rendering

import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.dsl.renderFields
import org.w3c.dom.Node

class RangeOwner(override val node: Node) : Component<Node>() {

    context(scope: NodeScope)
    fun afterBuild() {
        renderFields(*this@RangeOwner.children.toTypedArray())
    }

}