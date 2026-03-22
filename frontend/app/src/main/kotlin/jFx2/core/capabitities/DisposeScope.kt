package jFx2.core.capabilities

import jFx2.state.Disposable

class DisposeScope {
    private val bag = ArrayList<Disposable>()
    private var disposed = false

    fun register(d: Disposable) {
        if (disposed) { d.dispose(); return }
        bag.add(d)
    }

    fun register(cleanup: () -> Unit) {
        register(Disposable(cleanup))
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        for (d in bag.asReversed()) runCatching { d.dispose() }
        bag.clear()
    }
}