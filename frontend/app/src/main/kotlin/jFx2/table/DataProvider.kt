package jFx2.table

import jFx2.state.Property
import kotlinx.coroutines.*
import kotlin.math.floor

interface DataProvider<T> {
    val totalCount: Property<Int?>
    val sortState: Property<SortState?>  // NEW
    suspend fun loadRange(offset: Int, limit: Int): List<T>
}

@OptIn(ExperimentalCoroutinesApi::class)
class LazyTableModel<T>(
    private val scope: CoroutineScope,
    private val provider: DataProvider<T>,
    private val pageSize: Int = 200,
    private val prefetchPages: Int = 2
) {

    val totalCount: Property<Int?> get() = provider.totalCount

    /**
     * `totalCount` is nullable, but some callers intentionally start with `0`.
     * Before the first successful load, treat `0` as "unknown" so page 0 can be requested.
     * After the first successful load, `0` means "empty".
     */
    val hasLoadedOnce = Property(false)

    val invalidateTick = Property(0L)

    private val cache = HashMap<Int, List<T>>()                  // pageIndex -> items
    private val inflight = HashMap<Int, Deferred<List<T>>>()     // pageIndex -> request

    fun clearCache() {
        cache.clear()
        hasLoadedOnce.set(false)
        invalidateTick.set(invalidateTick.get() + 1)
    }

    private val dSort = provider.sortState.observe {
        clearCache()
    }

    fun setAll(items: List<T>) {
        clearCache()
        cache[0] = items
        hasLoadedOnce.set(true)
    }

    fun get(index: Int): T? {
        val pageIndex = floorDiv(index, pageSize)
        cache[pageIndex]?.let { page ->
            val local = index - pageIndex * pageSize
            return if (local in page.indices) page[local] else null
        }

        requestPage(pageIndex)
        for (i in 1..prefetchPages) {
            requestPage(pageIndex - i)
            requestPage(pageIndex + i)
        }
        return null
    }

    private fun requestPage(pageIndex: Int) {
        if (pageIndex < 0) return
        if (cache.containsKey(pageIndex)) return
        if (inflight.containsKey(pageIndex)) return

        val total = totalCount.get()
        val treatZeroAsUnknown = (total == 0 && !hasLoadedOnce.get())
        if (total != null && !treatZeroAsUnknown) {
            val start = pageIndex * pageSize
            if (start >= total) return
        }

        val d = scope.async(Dispatchers.Default) {
            provider.loadRange(pageIndex * pageSize, pageSize)
        }
        inflight[pageIndex] = d

        d.invokeOnCompletion { cause ->
            inflight.remove(pageIndex)
            if (cause == null) {
                val list = runCatching { d.getCompleted() }.getOrNull()
                if (list != null) {
                    cache[pageIndex] = list
                    hasLoadedOnce.set(true)
                    invalidateTick.set(invalidateTick.get() + 1)
                }
            }
        }
    }

    private fun floorDiv(a: Int, b: Int): Int = floor(a.toDouble() / b.toDouble()).toInt()
}
