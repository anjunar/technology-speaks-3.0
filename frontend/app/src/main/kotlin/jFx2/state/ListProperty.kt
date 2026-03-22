package jFx2.state

import kotlinx.serialization.Serializable

interface ReadOnlyListProperty<T> : ReadOnlyProperty<List<T>> {
    fun size(): Int = get().size
    operator fun get(index: Int): T = get()[index]
}

sealed class ListChange<T> {
    data class Add<T>(val fromIndex: Int, val items: List<T>) : ListChange<T>()
    data class Remove<T>(val fromIndex: Int, val items: List<T>) : ListChange<T>()
    data class Replace<T>(val fromIndex: Int, val old: List<T>, val new: List<T>) : ListChange<T>()
    data class Clear<T>(val old: List<T>) : ListChange<T>()
    data class SetAll<T>(val old: List<T>, val new: List<T>) : ListChange<T>()
}

@Serializable(with = ListPropertyAsListSerializer::class)
class ListProperty<T>(
    initial: List<T> = emptyList()
) : ReadOnlyListProperty<T>, MutableList<T> {

    private val backing: MutableList<T> = initial.toMutableList()

    private val valueListeners = LinkedHashMap<Int, (List<T>) -> Unit>()
    private val changeListeners = LinkedHashMap<Int, (ListChange<T>) -> Unit>()
    private var nextId = 1

    override fun get(): List<T> = backing.toList()

    override fun observe(listener: (List<T>) -> Unit): Disposable {
        val id = nextId++
        valueListeners[id] = listener
        listener(get())

        if (valueListeners.size > 100) {
            console.warn("Too many listeners on ${this::class.simpleName}")
        }

        return { valueListeners.remove(id) }
    }

    override fun observeWithoutInitial(listener: (List<T>) -> Unit): Disposable {
        val id = nextId++
        valueListeners[id] = listener

        if (valueListeners.size > 100) {
            console.warn("Too many listeners on ${this::class.simpleName}")
        }

        return { valueListeners.remove(id) }
    }

    fun observeChanges(listener: (ListChange<T>) -> Unit): Disposable {
        val id = nextId++
        changeListeners[id] = listener
        return { changeListeners.remove(id) }
    }

    fun setAll(items: Iterable<T>) {
        val old = backing.toList()
        backing.clear()
        backing.addAll(items)
        fireValue()
        fireChange(ListChange.SetAll(old, backing.toList()))
    }

    fun update(transform: (MutableList<T>) -> Unit) {
        val old = backing.toList()
        transform(backing)
        if (old == backing) return
        fireValue()
        fireChange(ListChange.SetAll(old, backing.toList()))
    }

    override val size: Int get() = backing.size

    override fun isEmpty(): Boolean = backing.isEmpty()

    override fun contains(element: T): Boolean = backing.contains(element)

    override fun containsAll(elements: Collection<T>): Boolean = backing.containsAll(elements)

    override fun get(index: Int): T = backing[index]

    override fun indexOf(element: T): Int = backing.indexOf(element)

    override fun lastIndexOf(element: T): Int = backing.lastIndexOf(element)

    override fun iterator(): MutableIterator<T> = object : MutableIterator<T> {
        private val it = backing.iterator()
        private var last: T? = null
        private var index = -1

        override fun hasNext(): Boolean = it.hasNext()

        override fun next(): T {
            val v = it.next()
            last = v
            index++
            return v
        }

        override fun remove() {
            val removed = last ?: error("remove() called before next()")
            it.remove()
            fireValue()
            fireChange(ListChange.Remove(index, listOf(removed)))
        }
    }

    override fun listIterator(): MutableListIterator<T> = listIterator(0)

    override fun listIterator(index: Int): MutableListIterator<T> {
        val it = backing.listIterator(index)
        var lastIndex = -1
        var lastValue: T? = null

        return object : MutableListIterator<T> {
            override fun hasNext(): Boolean = it.hasNext()
            override fun hasPrevious(): Boolean = it.hasPrevious()
            override fun nextIndex(): Int = it.nextIndex()
            override fun previousIndex(): Int = it.previousIndex()

            override fun next(): T {
                lastIndex = it.nextIndex()
                val v = it.next()
                lastValue = v
                return v
            }

            override fun previous(): T {
                lastIndex = it.previousIndex()
                val v = it.previous()
                lastValue = v
                return v
            }

            override fun remove() {
                val removed = lastValue ?: error("remove() called before next/previous")
                it.remove()
                fireValue()
                fireChange(ListChange.Remove(lastIndex, listOf(removed)))
            }

            override fun add(element: T) {
                val insertIndex = it.nextIndex()
                it.add(element)
                fireValue()
                fireChange(ListChange.Add(insertIndex, listOf(element)))
            }

            override fun set(element: T) {
                val old = lastValue ?: error("set() called before next/previous")
                it.set(element)
                fireValue()
                fireChange(ListChange.Replace(lastIndex, listOf(old), listOf(element)))
            }
        }
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> =
        backing.subList(fromIndex, toIndex).toMutableList()

    override fun add(element: T): Boolean {
        val from = backing.size
        val ok = backing.add(element)
        if (ok) {
            fireValue()
            fireChange(ListChange.Add(from, listOf(element)))
        }
        return ok
    }

    override fun add(index: Int, element: T) {
        backing.add(index, element)
        fireValue()
        fireChange(ListChange.Add(index, listOf(element)))
    }

    override fun addAll(elements: Collection<T>): Boolean {
        if (elements.isEmpty()) return false
        val from = backing.size
        val ok = backing.addAll(elements)
        if (ok) {
            fireValue()
            fireChange(ListChange.Add(from, elements.toList()))
        }
        return ok
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        if (elements.isEmpty()) return false
        val ok = backing.addAll(index, elements)
        if (ok) {
            fireValue()
            fireChange(ListChange.Add(index, elements.toList()))
        }
        return ok
    }

    override fun clear() {
        val old = backing.toList()
        backing.clear()
        fireValue()
        if (old.isEmpty()) return
        fireChange(ListChange.Clear(old))
    }

    override fun remove(element: T): Boolean {
        val idx = backing.indexOf(element)
        if (idx < 0) return false
        backing.removeAt(idx)
        fireValue()
        fireChange(ListChange.Remove(idx, listOf(element)))
        return true
    }

    override fun removeAt(index: Int): T {
        val removed = backing.removeAt(index)
        fireValue()
        fireChange(ListChange.Remove(index, listOf(removed)))
        return removed
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        if (elements.isEmpty() || backing.isEmpty()) return false

        val old = backing.toList()
        val ok = backing.removeAll(elements.toSet())
        if (!ok) return false

        fireValue()
        fireChange(ListChange.SetAll(old, backing.toList()))
        return true
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        val old = backing.toList()
        val ok = backing.retainAll(elements.toSet())
        if (!ok) return false

        fireValue()
        fireChange(ListChange.SetAll(old, backing.toList()))
        return true
    }

    override fun set(index: Int, element: T): T {
        val old = backing[index]
        if (old == element) return old
        backing[index] = element
        fireValue()
        fireChange(ListChange.Replace(index, listOf(old), listOf(element)))
        return old
    }

    private fun fireValue() {
        val snapshot = valueListeners.values.toList()
        val v = get() // snapshot list
        snapshot.forEach { it(v) }
    }

    private fun fireChange(change: ListChange<T>) {
        val snapshot = changeListeners.values.toList()
        snapshot.forEach { it(change) }
    }

    override fun toString(): String {
        return "ListProperty(backing=$backing)"
    }


}

fun <T> ListProperty<T>.subscribe(target: ListProperty<T>): Disposable {
    // initial
    target.setAll(get())

    return observeChanges { ch ->
        when (ch) {
            is ListChange.Add -> target.addAll(ch.fromIndex, ch.items)
            is ListChange.Remove -> repeat(ch.items.size) { target.removeAt(ch.fromIndex) }
            is ListChange.Replace -> {
                // simplest: remove old count then add new
                repeat(ch.old.size) { target.removeAt(ch.fromIndex) }
                target.addAll(ch.fromIndex, ch.new)
            }
            is ListChange.Clear -> target.clear()
            is ListChange.SetAll -> target.setAll(ch.new)
        }
    }
}

fun <T> ListProperty<T>.subscribeBidirectional(
    other: ListProperty<T>,
    initialToOther: Boolean = true
): Disposable {

    if (initialToOther) other.setAll(this.get()) else this.setAll(other.get())

    var guard = 0
    fun guarded(block: () -> Unit) {
        if (guard != 0) return
        guard++
        try { block() } finally { guard-- }
    }

    val d1 = this.observeChanges { ch ->
        guarded {
            when (ch) {
                is ListChange.Add -> other.addAll(ch.fromIndex, ch.items)
                is ListChange.Remove -> repeat(ch.items.size) { other.removeAt(ch.fromIndex) }
                is ListChange.Replace -> {
                    repeat(ch.old.size) { other.removeAt(ch.fromIndex) }
                    other.addAll(ch.fromIndex, ch.new)
                }
                is ListChange.Clear -> other.clear()
                is ListChange.SetAll -> other.setAll(ch.new)
            }
        }
    }

    val d2 = other.observeChanges { ch ->
        guarded {
            when (ch) {
                is ListChange.Add -> this.addAll(ch.fromIndex, ch.items)
                is ListChange.Remove -> repeat(ch.items.size) { this.removeAt(ch.fromIndex) }
                is ListChange.Replace -> {
                    repeat(ch.old.size) { this.removeAt(ch.fromIndex) }
                    this.addAll(ch.fromIndex, ch.new)
                }
                is ListChange.Clear -> this.clear()
                is ListChange.SetAll -> this.setAll(ch.new)
            }
        }
    }

    return {
        d1.dispose()
        d2.dispose()
    }
}
