package jFx2.virtual

import app.domain.core.AbstractEntity
import app.domain.core.Data
import app.domain.core.Table
import jFx2.state.Disposable
import jFx2.state.ListChange
import jFx2.state.ListProperty

abstract class RangeDataProvider<T : Data<out AbstractEntity>> {

    open val maxItems: Int = 5000
    open val pageSize: Int = 50

    val items = ListProperty<T>()

    var reachedEnd: Boolean = false
    var hasKnownCount: Boolean = false
    var knownCount: Int = 0
    val endReached: Boolean
        get() = reachedEnd || items.size >= maxItems

    val loadedCount: Int
        get() = items.size

    fun reload() {
        items.setAll(emptyList())
        reachedEnd = false
    }

    fun setAll(items: List<T>, size : Int) {
        this.items.setAll(items)
        knownCount = size
        hasKnownCount = true
    }

    fun getOrNull(index: Int): T? = items.getOrNull(index)

    abstract suspend fun fetch(index : Int, limit: Int): Table<out T>

    suspend fun ensureRange(from: Int, toInclusive: Int) {
        if (endReached) return
        if (toInclusive < 0) return
        if (toInclusive < items.size) return

        val target = kotlin.math.min(toInclusive, maxItems - 1)

        while (items.size <= target && !reachedEnd) {
            val remaining = target - items.size + 1
            val limit = kotlin.math.min(pageSize, remaining)
            if (limit <= 0) return

            val table = fetch(items.size, limit)

            if (table.rows.isEmpty()) {
                reachedEnd = true
                return
            }

            items += table.rows

            if (table.rows.size < limit) {
                reachedEnd = true
                return
            }
        }
    }

    fun observeChanges(listener: (ListChange<*>) -> Unit): Disposable =
        items.observeChanges { listener(it) }

    fun upsert(entity: T) {
        val id = entity.data.id?.get()
        if (id == null) {
            items.add(entity)
            return
        }

        val index = items.indexOfFirst { it.data.id?.get() == id }
        if (index >= 0) items[index] = entity else items.add(entity)
    }

    fun remove(entity: T) {
        items.remove(entity)
    }

}
