package app.domain.core

import kotlinx.serialization.Serializable

@Serializable
class Data<E>(
    val data: E
)