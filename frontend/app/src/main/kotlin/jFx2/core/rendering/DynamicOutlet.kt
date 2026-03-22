package jFx2.core.rendering

import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.state.Disposable
import jFx2.state.Property
import org.w3c.dom.Element

private class OutletOwner(override val node: Element) : Component<Element>()

context(scope: NodeScope)
fun dynamicOutlet(content: Property<Component<*>?>) {
    val host = scope.create<Element>("div")
    host.classList.add("dynamic-outlet")
    scope.parent.appendChild(host)

    val owner = OutletOwner(host)
    var current: Component<*>? = null

    fun detach(component: Component<*>) {
        runCatching { component.dispose() }
        component.node.parentNode?.removeChild(component.node)
        owner.removeChild(component)
    }

    fun render(component: Component<*>?) {
        if (component == null) {
            if (current != null) detach(current!!)
            return
        }
        if (current === component) return
        current?.let { detach(it) }
        current = component
        owner.addChild(component)
        host.appendChild(component.node)
    }

    val d: Disposable = content.observe { render(it) }
    scope.dispose.register(d)
    scope.dispose.register {
        current?.let { detach(it) }
        current = null
        host.parentNode?.removeChild(host)
    }
}