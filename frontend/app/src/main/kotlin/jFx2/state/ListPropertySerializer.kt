package jFx2.state

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class ListPropertySerializer<T>(
    private val elementSerializer: KSerializer<T>
) : KSerializer<ListProperty<T>> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("ListProperty") {
            element("items", ListSerializer(elementSerializer).descriptor)
        }

    override fun serialize(encoder: Encoder, value: ListProperty<T>) {
        val listSer = ListSerializer(elementSerializer)
        val items = value.toList() // snapshot
        encoder.encodeSerializableValue(listSer, items)
    }

    override fun deserialize(decoder: Decoder): ListProperty<T> {
        val listSer = ListSerializer(elementSerializer)
        val items = decoder.decodeSerializableValue(listSer)
        return ListProperty(items)
    }
}