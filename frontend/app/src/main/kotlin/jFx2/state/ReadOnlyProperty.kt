package jFx2.state

interface ReadOnlyProperty<T> {
    fun get(): T
    fun observe(listener: (T) -> Unit): Disposable
    fun observeWithoutInitial(listener: (T) -> Unit): Disposable
}

fun <T> ReadOnlyProperty<T>.subscribe(target: Property<T>): Disposable {
    return observe { v -> target.set(v) }
}