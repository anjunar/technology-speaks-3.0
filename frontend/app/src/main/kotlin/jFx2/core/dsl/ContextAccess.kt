package jFx2.core.dsl

import jFx2.core.capabilities.NodeScope
import jFx2.core.capabitities.ArrayFormOwnerKey
import jFx2.core.capabitities.FormContextKey
import jFx2.core.capabitities.FormOwnerKey
import jFx2.forms.Form
import jFx2.forms.FormField

context(scope: NodeScope)
fun registerField(name: String, field: FormField<*, *>) {
    val formOwner = runCatching { scope.ctx.get(FormOwnerKey) }.getOrNull()
    formOwner?.fields?.set(name, field)

    scope.dispose.register {
        if (formOwner?.fields?.get(name) === field) {
            formOwner.fields.remove(name)
        }
    }

}

context(scope: NodeScope)
fun registerSubForm(name: String, form: Form<*>) {
    val formOwner = runCatching { scope.ctx.get(FormOwnerKey) }.getOrNull()
    formOwner?.subForms?.set(name, form)

    scope.dispose.register {
        if (formOwner?.subForms?.get(name) === form) {
            formOwner.subForms.remove(name)
        }
    }
}

context(scope: NodeScope)
fun registerSubForm(index: Int, form: Form<*>) {
    val formOwner = runCatching { scope.ctx.get(ArrayFormOwnerKey) }.getOrNull()
    formOwner?.registerSubForm(index, form)

    scope.dispose.register {
        formOwner?.unregisterSubForm(form)
    }

}
