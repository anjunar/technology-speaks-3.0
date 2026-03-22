package jFx2.controls

import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dom.ElementInsertPoint
import org.w3c.dom.HTMLImageElement

@JfxComponentBuilder
class Image(override val node: HTMLImageElement) : Component<HTMLImageElement>() {

    var src : String
        get() = node.src
        set(value) { node.src = value }

}