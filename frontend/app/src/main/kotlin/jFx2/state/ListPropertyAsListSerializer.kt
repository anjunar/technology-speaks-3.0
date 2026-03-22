package jFx2.state

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ListPropertyAsListSerializer : KSerializer<ListProperty<Any?>> {

    override val descriptor: SerialDescriptor =
        ListSerializer<Any>(kotlinx.serialization.serializer()).descriptor

    override fun serialize(encoder: Encoder, value: ListProperty<Any?>) {
        encoder.encodeSerializableValue(
            ListSerializer(kotlinx.serialization.serializer()),
            value.toList()
        )
    }

    override fun deserialize(decoder: Decoder): ListProperty<Any?> {
        val items = decoder.decodeSerializableValue(
            ListSerializer<Any>(kotlinx.serialization.serializer())
        )
        return ListProperty(items)
    }
}