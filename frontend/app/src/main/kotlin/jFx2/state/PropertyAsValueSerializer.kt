package jFx2.state

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

object PropertyAsValueSerializer : KSerializer<Property<Any?>> {

    private val anySer: KSerializer<Any?> = serializer()

    override val descriptor: SerialDescriptor = anySer.descriptor

    override fun serialize(encoder: Encoder, value: Property<Any?>) {
        encoder.encodeSerializableValue(anySer, value.get())
    }

    override fun deserialize(decoder: Decoder): Property<Any?> {
        val v = decoder.decodeSerializableValue(anySer)
        return Property(v)
    }
}
