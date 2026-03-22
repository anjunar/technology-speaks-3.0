package jFx2.forms

import jFx2.core.Component
import jFx2.state.Disposable
import jFx2.state.ListProperty
import org.w3c.dom.Element

abstract class FormField<V, N : Element> : Component<N>() {
    abstract override val node: N

    abstract val name : String

    val statusProperty = ListProperty<String>()
    val errorsProperty = ListProperty<String>()

    abstract var disabled : Boolean

    abstract fun read(): V
    abstract fun observeValue(listener: (V) -> Unit): Disposable
}

