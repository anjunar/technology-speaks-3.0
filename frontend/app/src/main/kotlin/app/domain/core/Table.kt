package app.domain.core

import kotlinx.serialization.Serializable

@Serializable
data class Table<E>(val rows : List<E> = emptyList(), val size : Int)