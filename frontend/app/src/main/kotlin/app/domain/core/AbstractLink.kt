package app.domain.core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("id")
@Serializable
abstract class AbstractLink() {
    abstract val id : String
    abstract val rel : String
    abstract val url : String
    abstract val method : String

    abstract val name : String
    abstract val icon : String
}