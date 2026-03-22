package jFx2.core.dsl

import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.state.Property
import org.w3c.dom.HTMLElement
import org.w3c.dom.css.CSSStyleDeclaration
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent

context(scope: NodeScope)
fun renderComponent(field: Component<*>) {
    if (field.node.parentNode !== scope.insertPoint.parent) {
        scope.insertPoint.insert(field.node)
        field.mount()
    }
    scope.dispose.register {
        runCatching { field.dispose() }
        field.node.parentNode?.removeChild(field.node)
    }
}

context(scope: NodeScope)
fun renderFields(vararg field: Component<*>) {
    for (f in field) renderComponent(f)
}


context(scope: NodeScope)
fun className(block: () -> String) {
    var disposed = false

    fun scheduleNextFlush() {
        if (disposed) return
        scope.ui.build.dirty {
            if (disposed) return@dirty
            (scope.parent as HTMLElement).className = block()
        }
        scope.ui.build.afterBuild {
            scheduleNextFlush()
        }
    }

    scheduleNextFlush()
    scope.dispose.register { disposed = true }
}

context(scope: NodeScope)
fun className(value : String) {
    (scope.parent as HTMLElement).className = value
}


context(scope: NodeScope)
fun zIndex(value: Property<Int>) {
    scope.dispose.register(value.observe { (scope.parent as HTMLElement).style.zIndex = it.toString() })
}

context(scope: NodeScope)
fun style(block: CSSStyleDeclaration.() -> Unit) {
    var disposed = false

    fun scheduleNextFlush() {
        if (disposed) return
        scope.ui.build.dirty {
            if (disposed) return@dirty
            (scope.parent as HTMLElement).style.unsafeCast<CSSStyleDeclaration>().block()
        }
        scope.ui.build.afterBuild {
            scheduleNextFlush()
        }
    }

    scheduleNextFlush()
    scope.dispose.register { disposed = true }
}

@Suppress("UNCHECKED_CAST")
context(scope: NodeScope)
fun mousedown(block: (MouseEvent) -> Unit) {
    val listener = block as (Event) -> Unit
    scope.parent.addEventListener("mousedown", listener)
    scope.dispose.register { scope.parent.removeEventListener("mousedown", listener) }
}

@Suppress("UNCHECKED_CAST")
context(scope: NodeScope)
fun onClick(block: (MouseEvent) -> Unit) {
    val listener = block as (Event) -> Unit
    scope.parent.addEventListener("click", {
        listener(it)
        scope.ui.build.flush()
    })
    scope.dispose.register { scope.parent.removeEventListener("click", listener) }
}