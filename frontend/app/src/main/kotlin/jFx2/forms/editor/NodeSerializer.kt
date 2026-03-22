package jFx2.forms.editor

import jFx2.state.Property
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonObject

class NodeSerializer : KSerializer<Property<EditorNode>> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): Property<EditorNode> {
        val jsonDecoder =
            decoder as? JsonDecoder ?: throw SerializationException("JsInterfaceSerializer works only with JSON")
        val element = jsonDecoder.decodeJsonElement()
        val jsObj = js("JSON.parse")(element.toString())
        @Suppress("UNCHECKED_CAST")
        return Property(jsObj.unsafeCast<EditorNode>())
    }


    override fun serialize(encoder: Encoder, value: Property<EditorNode>) {
        val jsonEncoder =
            encoder as? JsonEncoder ?: throw SerializationException("JsInterfaceSerializer works only with JSON")
        val jsonString = js("JSON.stringify")(value.get())
        jsonEncoder.encodeJsonElement(encoder.json.parseToJsonElement(jsonString))
    }
}