package jFx2.table

enum class SortDirection { ASC, DESC }

data class SortState(
    val columnId: String,
    val direction: SortDirection
)