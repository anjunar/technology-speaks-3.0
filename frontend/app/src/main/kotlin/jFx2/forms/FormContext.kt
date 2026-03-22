package jFx2.forms

class FormContext(
    private val parent: FormContext?,
    private val namespace: String?
) {
    private val fields: MutableMap<String, FormField<*, *>> = LinkedHashMap()
    private val subForms: MutableMap<String, Form<*>> = LinkedHashMap()

    private fun qualify(name: String): String =
        if (namespace.isNullOrBlank()) name else "$namespace.$name"

    fun registerField(name: String, field: FormField<*, *>) {
        val qualified = qualify(name)
        fields[qualified] = field
        parent?.registerField(qualified, field)
    }

    fun unregisterField(name: String, field: FormField<*, *>) {
        val qualified = qualify(name)
        if (fields[qualified] === field) {
            fields.remove(qualified)
        }
        parent?.unregisterField(qualified, field)
    }

    fun registerSubForm(name: String, form: Form<*>) {
        val qualified = qualify(name)
        subForms[qualified] = form
        parent?.registerSubForm(qualified, form)
    }

    fun unregisterSubForm(name: String, form: Form<*>) {
        val qualified = qualify(name)
        if (subForms[qualified] === form) {
            subForms.remove(qualified)
        }
        parent?.unregisterSubForm(qualified, form)
    }
}
