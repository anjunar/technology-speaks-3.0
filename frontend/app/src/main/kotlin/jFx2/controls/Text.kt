package jFx2.controls

import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.state.Disposable
import jFx2.state.Property
import kotlinx.browser.document
import org.w3c.dom.Text

class TextNode(override val node: Text) : Component<Text>() {
    fun set(value: String) {
        node.data = value
    }
}

context(scope: NodeScope)
fun textContent(value: String) {
    scope.parent.textContent = value
}

context(scope: NodeScope)
fun text(value: String): TextNode {
    val tn = TextNode(document.createTextNode(value))
    scope.parent.appendChild(tn.node)
    scope.dispose.register { tn.node.parentNode?.removeChild(tn.node) }
    return tn
}

context(scope: NodeScope)
fun <T> text(
    dep: Property<T>,
    map: (T) -> String = { it.toString() }
): TextNode {
    val tn = TextNode(document.createTextNode(map(dep.get())))
    scope.parent.appendChild(tn.node)

    val d = dep.observe { v -> tn.set(map(v)) }
    scope.dispose.register(d)
    scope.dispose.register { tn.node.parentNode?.removeChild(tn.node) }

    return tn
}

context(scope: NodeScope)
fun text(
    vararg deps: Property<*>,
    compute: () -> String
): TextNode {
    val tn = TextNode(document.createTextNode(compute()))
    scope.parent.appendChild(tn.node)

    val ds = ArrayList<Disposable>(deps.size)
    for (p in deps) {
        ds += p.observe { tn.set(compute()) }
    }

    for (d in ds) scope.dispose.register(d)
    scope.dispose.register { tn.node.parentNode?.removeChild(tn.node) }

    return tn
}
