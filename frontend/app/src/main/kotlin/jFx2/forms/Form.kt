@file:OptIn(ExperimentalJsReflectionCreateInstance::class)
@file:Suppress("UNCHECKED_CAST")

package jFx2.forms

import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.capabitities.FormContextKey
import jFx2.core.capabitities.FormOwnerKey
import jFx2.core.dom.ElementInsertPoint
import jFx2.core.dsl.registerSubForm
import jFx2.core.dsl.renderFields
import jFx2.decodeURIComponent
import jFx2.state.JobRegistry
import jFx2.state.Property
import kotlinx.browser.window
import org.w3c.dom.HTMLFormElement
import kotlin.reflect.KClass
import kotlin.reflect.createInstance

class Form<E : Any>(override val node: HTMLFormElement, var model : E, val clazz : KClass<E>) : Component<HTMLFormElement>(), Formular {

    val fields: MutableMap<String, FormField<*, *>> = LinkedHashMap()
    val subForms: MutableMap<String, Form<*>> = LinkedHashMap()

    var submitHandler: (suspend () -> Unit)? = null

    val editable = Property(true)

    fun onSubmit(handler: suspend () -> Unit) { submitHandler = handler }

    var disabled : Boolean
        get() = ! editable.get()
        set(v) {
            editable.set(!v)
        }

    fun setErrors(errors : List<ErrorResponse>) {
        for (response in errors) {

            response.path.firstOrNull()?.let {

                val formField = fields[it]

                if (formField == null) {
                    val subForm = subForms[it]
                    subForm?.setErrors(listOf(ErrorResponse(response.message, response.path.drop(1))))
                } else {
                    formField.errorsProperty.add(response.message)
                }


            }

        }
    }

    context(scope: NodeScope)
    fun initialize() {
        renderFields(*this@Form.children.toTypedArray())

        onDispose(editable.observe { v ->
            if (v) {
                node.removeAttribute("disabled")
            } else {
                node.setAttribute("disabled", "true")
            }

            fields.values.toList().forEach { it.disabled = !v }
            subForms.values.toList().forEach { it.disabled = !v }
        })


        node.setAttribute("action", window.location.pathname + window.location.search)

        node.addEventListener("submit", {
            event -> event.preventDefault();

            window.history.pushState(null, "", node.action)

            JobRegistry.instance.launch("Form", "Form") {
                submitHandler?.invoke()
            }
        })
    }

    internal fun registerField(name: String, field: FormField<*, *>) {
        fields[name] = field
    }

    internal fun unregisterField(name: String) { fields.remove(name) }

    internal fun registerSubForm(namespace: String, sub: Form<*>) {
        subForms[namespace] = sub
    }

    internal fun unregisterSubForm(namespace: String) { subForms.remove(namespace) }
}

class FormNoop

context(scope: NodeScope)
fun <E : Any>form(
    namespace: String? = null,
    model : E? = FormNoop() as E?,
    clazz : KClass<E> = FormNoop::class as KClass<E>,
    block: context(NodeScope) Form<E>.() -> Unit
): Form<*> {
    val el = scope.create<HTMLFormElement>("form")

    val newModel = if (model == null) {
        el.setAttribute("disabled", "true")
        clazz.createInstance()
    } else {
        model
    }

    val c = Form(el, newModel, clazz)
    scope.attach(c)

    val childScope = scope.fork(
        parent = c.node,
        owner = c,
        ctx = scope.ctx.fork().also {
            it.set(FormOwnerKey, c)
        },
        ElementInsertPoint(c.node)
    )

    block(childScope, c)

    with(childScope) {
        scope.ui.build.afterBuild { c.initialize() }
    }

    return c
}

context(scope: NodeScope)
fun <E : Any>subForm(
    namespace: String = "",
    index : Int = -1,
    model : E?,
    clazz : KClass<E>,
    block: context(NodeScope) Form<E>.() -> Unit
): Form<E> {
    val el = scope.create<HTMLFormElement>("fieldset")

    val newModel = if (model == null) {
        el.setAttribute("disabled", "true")
        clazz.createInstance()
    } else {
        model
    }


    val c = Form(el, newModel, clazz)
    scope.attach(c)

    val childScope = scope.fork(
        parent = c.node,
        owner = c,
        ctx = scope.ctx.fork().also {
            it.set(FormOwnerKey, c)
        },
        ElementInsertPoint(c.node)
    )

    block(childScope, c)

    if (index > -1) {
        registerSubForm(index, c)
    } else {
        registerSubForm(namespace, c)
    }

    with(childScope) {
        scope.ui.build.afterBuild { c.initialize() }
    }


    return c
}
