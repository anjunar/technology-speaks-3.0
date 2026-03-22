package jFx2.controls

import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.capabilities.UiScope
import jFx2.core.dom.ElementInsertPoint
import org.w3c.dom.HTMLHeadingElement
import org.w3c.dom.HTMLSpanElement

class Heading(override val node: HTMLHeadingElement) : Component<HTMLHeadingElement>()

context(scope: NodeScope)
fun heading(level : Int, block: context(NodeScope) Heading.() -> Unit = {}): Heading {
    val el = scope.create<HTMLHeadingElement>("h$level")
    val c = Heading(el)
    scope.attach(c)

    val childScope = scope.fork(parent = c.node, owner = c, ctx = scope.ctx, ElementInsertPoint(c.node))
    block(childScope, c)

    return c
}
