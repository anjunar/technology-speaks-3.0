package jFx2.core.dsl

import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.state.Disposable
import jFx2.state.ListProperty
import jFx2.state.Property
import jFx2.state.subscribe
import jFx2.state.subscribeBidirectional

context(scope: NodeScope)
fun <T> subscribe(source: Property<T>, target: Property<T>): Disposable {
    val d = source.subscribe(target)
    scope.dispose.register{ d.dispose() }
    return d
}

context(scope: NodeScope)
fun <T> subscribeBidirectional(a: Property<T>, b: Property<T>): Disposable {
    val d = a.subscribeBidirectional(b)
    scope.dispose.register{ d.dispose() }
    return d
}

// -------------------- ListProperty --------------------

context(scope: NodeScope)
fun <T> subscribe(source: ListProperty<T>, target: ListProperty<T>): Disposable {
    val d = source.subscribe(target)
    scope.dispose.register{ d.dispose() }
    return d
}

context(scope: NodeScope)
fun <T> subscribeBidirectional(a: ListProperty<T>, b: ListProperty<T>): Disposable {
    val d = a.subscribeBidirectional(b)
    scope.dispose.register{ d.dispose() }
    return d
}