package jFx2.state

import kotlinx.serialization.Serializable

@Serializable(with = PropertyAsValueSerializer::class)
class Property<T>(var value: T) : ReadOnlyProperty<T> {
    private val listeners = LinkedHashMap<Int, (T) -> Unit>()
    private var nextId = 1

    override fun get(): T = value

    fun set(newValue: T) {
        if (newValue == value) return
        value = newValue
        listeners.values.toList().forEach { it(newValue) }
    }

    override fun observe(listener: (T) -> Unit): Disposable {
        val id = nextId++
        listeners[id] = listener
        listener(value)

        if (listeners.size > 100) {
            console.warn("Too many listeners on ${this::class.simpleName} : ${listeners.size}")
        }

        return { listeners.remove(id) }
    }

    override fun observeWithoutInitial(listener: (T) -> Unit): Disposable {
        val id = nextId++
        listeners[id] = listener

        if (listeners.size > 100) {
            console.warn("Too many listeners on ${this::class.simpleName} : ${listeners.size}")
        }

        return { listeners.remove(id) }
    }

    override fun toString(): String {
        return "Property(value=$value)"
    }


}

fun <T> Property<T>.subscribeBidirectional(
    other: Property<T>,
    initialToOther: Boolean = true
): Disposable {

    if (initialToOther) other.set(this.get()) else this.set(other.get())

    var guard = 0
    fun guarded(block: () -> Unit) {
        if (guard != 0) return
        guard++
        try { block() } finally { guard-- }
    }

    val d1 = this.observe { v -> guarded { other.set(v) } }
    val d2 = other.observe { v -> guarded { this.set(v) } }

    return {
        d1.dispose()
        d2.dispose()
    }
}