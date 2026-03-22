package jFx2.state

fun interface Disposable { fun dispose() }

class CompositeDisposable : Disposable {
    private val items = ArrayList<Disposable>()
    private var disposed = false

    fun add(d: Disposable) {
        if (disposed) { d.dispose(); return }
        items.add(d)
    }

    override fun dispose() {
        if (disposed) return
        disposed = true
        for (i in items.asReversed()) runCatching { i.dispose() }
        items.clear()
    }
}
