package app.domain.core

import kotlinx.serialization.Serializable

@Serializable
data class Link(
    val rel: String,
    val url: String,
    val method: String
)