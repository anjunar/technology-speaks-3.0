package jFx2.forms

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

enum class Status { valid, invalid, dirty, empty, focus }

@Serializable
data class ErrorResponse(val message: String, @Serializable(with = AnyListSerializer::class) val path : List<Any>) {
    companion object {
        object AnyListSerializer : KSerializer<List<Any>> {

            override val descriptor =
                ListSerializer(JsonElement.serializer()).descriptor

            override fun serialize(encoder: Encoder, value: List<Any>) {
                val jsonEncoder = encoder as JsonEncoder
                val elements = value.map {
                    when (it) {
                        is String -> JsonPrimitive(it)
                        is Number -> JsonPrimitive(it)
                        else -> error("Unsupported type: ${it::class}")
                    }
                }
                jsonEncoder.encodeSerializableValue(
                    ListSerializer(JsonElement.serializer()),
                    elements
                )
            }

            override fun deserialize(decoder: Decoder): List<Any> {
                val jsonDecoder = decoder as JsonDecoder
                val elements = jsonDecoder.decodeSerializableValue(
                    ListSerializer(JsonElement.serializer())
                )

                return elements.map {
                    val primitive = it.jsonPrimitive
                    primitive.intOrNull
                        ?: primitive.doubleOrNull
                        ?: primitive.content
                }
            }
        }
    }
}

interface Validator {
    fun validate(value: String): Boolean
    fun message(): String
}

class SizeValidator(val min: Int, val max: Int) : Validator {
    override fun validate(value: String): Boolean = value.length in min..max
    override fun message(): String = "Größe muss zwischen $min und $max sein"
}

class PatternValidator(val pattern: String) : Validator {
    override fun validate(value: String): Boolean = value.matches(pattern.toRegex())
    override fun message(): String = "Muss Pattern übereinstimmen '$pattern'"
}

class NotBlankValidator : Validator {
    override fun validate(value: String): Boolean = value.isNotBlank()
    override fun message(): String = "Darf nicht leer sein"
}

class EmailValidator : Validator {
    override fun validate(value: String): Boolean = value.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)$"))
    override fun message(): String = "Muss eine gültige Email sein"
}
