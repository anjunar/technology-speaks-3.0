package jFx2.table

import jFx2.state.Property
import jFx2.state.ReadOnlyProperty
import kotlin.math.max
import kotlin.math.min

enum class SelectionMode { SINGLE, MULTIPLE }

class SelectionModel(
    mode: SelectionMode = SelectionMode.SINGLE
) {
    val mode = Property(mode)

    private var anchor: Int? = null

    private val _selected = Property(setOf<Int>())
    val selected: ReadOnlyProperty<Set<Int>> get() = _selected

    val selectedIndex = Property<Int?>(null)

    fun isSelected(index: Int): Boolean = _selected.get().contains(index)

    fun clearSelection() {
        _selected.set(emptySet())
        selectedIndex.set(null)
        anchor = null
    }

    fun select(index: Int, additive: Boolean = false, range: Boolean = false) {
        if (index < 0) return

        val modeNow = mode.get()

        if (modeNow == SelectionMode.SINGLE) {
            _selected.set(setOf(index))
            selectedIndex.set(index)
            anchor = index
            return
        }

        val current = _selected.get()

        if (range) {
            val a = anchor ?: selectedIndex.get() ?: index
            val lo = min(a, index)
            val hi = max(a, index)
            val ranged = (lo..hi).toSet()

            // JavaFX-ish: shift replaces selection unless additive is also pressed
            val next = if (additive) current + ranged else ranged
            _selected.set(next)
            selectedIndex.set(index)
            // keep anchor stable
            if (anchor == null) anchor = a
            return
        }

        if (additive) {
            val next =
                if (current.contains(index)) current - index else current + index
            _selected.set(next)
            selectedIndex.set(index)
            anchor = index
            return
        }

        _selected.set(setOf(index))
        selectedIndex.set(index)
        anchor = index
    }
}

class FocusModel {
    val focusedIndex = Property<Int?>(null)
    fun focus(index: Int?) = focusedIndex.set(index)
}
