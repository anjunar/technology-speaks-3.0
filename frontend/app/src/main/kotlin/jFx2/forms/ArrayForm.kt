package jFx2.forms

import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.capabitities.ArrayFormOwnerKey
import jFx2.core.capabitities.FormContextKey
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dom.ElementInsertPoint
import org.w3c.dom.HTMLFieldSetElement

@JfxComponentBuilder(classes = ["array-form"], tag = "fieldset")
class ArrayForm(override val node: HTMLFieldSetElement) : Component<HTMLFieldSetElement>(), Formular {

    val subForms: MutableList<Form<*>> = mutableListOf()

    internal fun registerSubForm(index : Int, form: Form<*>) {
        subForms.add(index, form)
    }

    internal fun unregisterSubForm(form: Form<*>) {
        subForms.remove(form)
    }

}