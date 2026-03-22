package jFx2.core

import jFx2.state.CompositeDisposable
import jFx2.state.Disposable
import org.w3c.dom.Node

abstract class Component<N : Node> {

    abstract val node: N

    val disposeBag = CompositeDisposable()

    val children = ArrayList<Component<*>>()

    open fun mount() {}

    fun onDispose(d: Disposable) = disposeBag.add(d)

    fun addChild(child: Component<*>) {
        children.add(child)
    }

    fun removeChild(child: Component<*>) {
        children.remove(child)
    }

    open fun dispose() {
        disposeBag.dispose()
    }
}

