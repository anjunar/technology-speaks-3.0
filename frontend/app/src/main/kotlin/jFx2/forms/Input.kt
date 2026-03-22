package jFx2.forms

import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.state.Disposable
import jFx2.state.ListChange
import jFx2.state.ListProperty
import jFx2.state.Property
import org.w3c.dom.Element
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event

@JfxComponentBuilder
class Input(
    override val name: String,
    override val node: HTMLInputElement
) : FormField<String, HTMLInputElement>(), HasPlaceholder {

    val validatorsProperty = ListProperty<Validator>()
    val valueProperty = Property(node.value)

    val valueAsNumberProperty = Property(node.valueAsNumber)
    val editable = Property(!node.disabled)

    init {
        node.name = name
    }

    override var disabled: Boolean
        get() = ! editable.get()
        set(value) {
            editable.set(!value)
        }

    fun addValidator(validator: Validator) {
        validatorsProperty.add(validator)
    }

    fun onChange(callback : (Event) -> Unit) {
        node.onchange = callback
    }

    fun type(type : String) {
        node.type = type
    }

    override fun observeValue(listener: (String) -> Unit): Disposable = valueProperty.observe(listener)

    context(scope: NodeScope)
    fun afterBuild() {
        val defaultValue = valueProperty.get()

        valueProperty.observe {
            if (node.type != "file") {
                node.value = it
            }
        }

        valueAsNumberProperty.observe {
            if (node.type == "number") {
                node.valueAsNumber = it
            }
        }

        editable.observe {
            node.readOnly = !it
        }

        node.addEventListener("change", {
            editable.set(! node.readOnly)
        })

        node.addEventListener("input", {
            val v = node.value

            if (v.isBlank()) statusProperty.add(Status.empty.name) else statusProperty.remove(Status.empty.name)
            if (v != defaultValue) statusProperty.add(Status.dirty.name) else statusProperty.remove(Status.dirty.name)

            valueProperty.set(v)

            if (node.type == "number") {
                valueAsNumberProperty.set(node.valueAsNumber)
            }

            validate()
            scope.ui.build.flush()
        })

        node.addEventListener("focus", {
            statusProperty.add(Status.focus.name)
            scope.ui.build.flush()
        })
        node.addEventListener("blur", {
            statusProperty.remove(Status.focus.name)
            scope.ui.build.flush()
        })

        onDispose(bindStatusClasses(node, statusProperty))
    }

    fun validate() {
        val errors = validatorsProperty.get().filter { !it.validate(node.value) }
        if (errors.isNotEmpty()) {
            statusProperty.add(Status.invalid.name)
            statusProperty.remove(Status.valid.name)
        } else {
            statusProperty.remove(Status.invalid.name)
            statusProperty.add(Status.valid.name)
        }
        errorsProperty.setAll(errors.map { it.message() })
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
                is ListChange.Replace -> { change.old.forEach(::remove); change.new.forEach(::add) }
                is ListChange.Clear -> { for (c in owned) node.classList.remove(c); owned.clear() }
                is ListChange.SetAll -> resync(change.new)
            }
        }
    }

    override var placeholder: String
        get() = node.placeholder
        set(v) { node.placeholder = v }

    override fun read(): String = node.value

}