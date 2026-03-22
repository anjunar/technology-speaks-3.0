package jFx2.forms

import jFx2.controls.Span
import jFx2.layout.div
import jFx2.controls.span
import jFx2.controls.text
import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dom.ElementInsertPoint
import jFx2.core.dsl.className
import jFx2.core.dsl.renderComponent
import jFx2.core.template
import jFx2.layout.hr
import jFx2.state.Disposable
import jFx2.state.ListChange
import jFx2.state.ListProperty
import org.w3c.dom.Element
import org.w3c.dom.HTMLDivElement

@JfxComponentBuilder(classes = ["input-container"])
class InputContainer(
    override val node: HTMLDivElement,
    val placeholder: String
) : Component<HTMLDivElement>() {

    private lateinit var field: FormField<*, *>
    private lateinit var errorsSpan: Span

    context(scope: NodeScope)
    fun afterBuild() {
        children.firstOrNull()?.let { field = it as FormField<*, *> } ?: error("No field found in container")
        (field as? HasPlaceholder)?.placeholder = placeholder

        onDispose(field.errorsProperty.observeChanges { syncErrors() })
        syncErrors()

        template {
            div {
                className { "label" }

                span {
                    className { "placeholder" }

                    onDispose(bindStatusClasses(node, field.statusProperty))

                    text { placeholder }
                }
            }

            div {
                renderComponent(field)
            }

            hr {
                onDispose(bindStatusClasses(node, field.statusProperty))
            }

            div {
                className {
                    "errors"
                }
                errorsSpan = span {}
            }

            onDispose(field.observeValue { scope.ui.build.flush() })
        }
    }

    private fun syncErrors() {
        if (!::errorsSpan.isInitialized) return
        errorsSpan.node.textContent = field.errorsProperty.get().joinToString(" ")
    }

    private fun bindStatusClasses(node: Element, status: ListProperty<String>): Disposable {
        val owned = LinkedHashSet<String>()

        fun add(cls: String) {
            val c = cls.trim()
            if (c.isEmpty()) return
            if (owned.add(c)) node.classList.add(c)
        }

        fun remove(cls: String) {
            val c = cls.trim()
            if (c.isEmpty()) return
            if (owned.remove(c)) node.classList.remove(c)
        }

        fun resync(items: List<String>) {
            for (c in owned) node.classList.remove(c)
            owned.clear()
            for (c in items) add(c)
        }

        resync(status.get())

        return status.observeChanges { change ->
            when (change) {
                is ListChange.Add -> change.items.forEach(::add)
                is ListChange.Remove -> change.items.forEach(::remove)
                is ListChange.Replace -> {
                    change.old.forEach(::remove)
                    change.new.forEach(::add)
                }
                is ListChange.Clear -> {
                    for (c in owned) node.classList.remove(c)
                    owned.clear()
                }
                is ListChange.SetAll -> resync(change.new)
            }
        }
    }
}