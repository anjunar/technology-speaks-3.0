package jFx2.forms

import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dom.ElementInsertPoint
import jFx2.core.dsl.registerField
import jFx2.core.dsl.renderFields
import jFx2.state.Disposable
import jFx2.state.Property
import org.w3c.dom.HTMLSelectElement

@JfxComponentBuilder
class Select(override val node: HTMLSelectElement, override val name: String) : FormField<String?, HTMLSelectElement>() {

    val value = Property<String?>(null)

    override var disabled: Boolean
        get() = node.disabled
        set(value) {
            node.disabled = value
        }

    context(scope: NodeScope)
    fun afterBuild() {
        node.onchange = { value.set(node.value) }

        observeValue { node.value = it ?: "" }

        renderFields(*this@Select.children.toTypedArray())
    }

    override fun read(): String? = value.get()

    override fun observeValue(listener: (String?) -> Unit): Disposable = value.observe(listener)

}