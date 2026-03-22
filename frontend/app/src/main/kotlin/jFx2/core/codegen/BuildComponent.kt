package jFx2.core.codegen

import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.capabitities.ArrayFormOwnerKey
import jFx2.core.capabitities.FormContextKey
import jFx2.core.dom.DomInsertPoint
import jFx2.core.dom.ElementInsertPoint
import jFx2.core.dsl.registerField
import jFx2.core.dsl.registerSubForm
import jFx2.forms.ArrayForm
import jFx2.forms.FormContext
import jFx2.forms.FormField
import jFx2.forms.Formular
import org.w3c.dom.Element
import org.w3c.dom.Node

context(scope: NodeScope)
inline fun <E : Element, C : Component<E>> buildComponent(
    tag: String,
    classes: Array<String> = emptyArray(),
    crossinline create: (E) -> C,
    crossinline block: context(NodeScope) C.() -> Unit = {},
    insertPointFactory: (Node) -> DomInsertPoint = { ElementInsertPoint(it) },
    afterBuild: AfterBuildMode = AfterBuildMode.NONE,
    crossinline afterBuildCall: context(NodeScope) C.() -> Unit = {}
): C {
    val el = scope.create<E>(tag)
    for (cls in classes) {
        val c = cls.trim()
        if (c.isNotEmpty()) el.classList.add(c)
    }

    val component = create(el)

    scope.attach(component)


    val childScope = when (component) {
        is FormField<*, *> -> {
            registerField(component.name, component)
            scope.fork(
                parent = component.node,
                owner = component,
                ctx = scope.ctx,
                insertPoint = insertPointFactory(component.node)
            )
        }
        is ArrayForm -> {
            scope.fork(
                parent = component.node,
                owner = component,
                ctx = scope.ctx.fork().also {
                    it.set(ArrayFormOwnerKey, component)
                },
                ElementInsertPoint(component.node)
            )
        }
        else -> {
            scope.fork(
                parent = component.node,
                owner = component,
                ctx = scope.ctx,
                insertPoint = insertPointFactory(component.node)
            )
        }
    }

    component.onDispose { childScope.dispose.dispose() }

    block(childScope, component)

    when (afterBuild) {
        AfterBuildMode.NONE -> Unit
        AfterBuildMode.EAGER -> afterBuildCall(childScope, component)
        AfterBuildMode.SCHEDULED -> scope.ui.build.afterBuild { afterBuildCall(childScope, component) }
    }

    return component
}
