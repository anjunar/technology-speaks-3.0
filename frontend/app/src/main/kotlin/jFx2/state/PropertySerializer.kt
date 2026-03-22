package jFx2.state

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

class PropertySerializer<T>(
    private val valueSerializer: KSerializer<T>
) : KSerializer<Property<T>> {

    override val descriptor: SerialDescriptor = valueSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Property<T>) {
        valueSerializer.serialize(encoder, value.get())
    }

    override fun deserialize(decoder: Decoder): Property<T> {
        val v = valueSerializer.deserialize(decoder)
        return Property(v)
    }
}
