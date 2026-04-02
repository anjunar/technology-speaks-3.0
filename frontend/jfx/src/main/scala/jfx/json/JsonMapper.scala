package jfx.json

import jfx.json.deserializer.{Deserializer, DeserializerFactory, JsonContext, ModelDeserializer}
import jfx.json.serializer.{JavaContext, ModelSerializer, Serializer, SerializerFactory}

import scala.scalajs.js
import scala.scalajs.js.Dynamic

object JsonMapper {
  def deserialize[M](json: Dynamic, meta: reflect.TypeDescriptor): M =
    new ModelDeserializer().deserialize(json, new JsonContext(meta)).asInstanceOf[M]

  def deserializeArray[M](json: js.Array[js.Dynamic], meta: reflect.TypeDescriptor): Seq[M] =
    if (json == null || js.isUndefined(json)) Seq.empty
    else {
      val builder = Seq.newBuilder[M]
      var i = 0
      while (i < json.length) {
        builder += deserialize(json(i), meta)
        i += 1
      }
      builder.result()
    }

  def serialize(model: Any): Dynamic = {
    serialize(model, null)
  }

  def serialize[M](model: M, meta: reflect.TypeDescriptor): Dynamic = {
    val typeDescriptor = if (meta != null) {
      meta
    } else {
      val typeName = model.getClass.getName
      reflect.ReflectRegistry.loadClass(typeName).orElse(
        reflect.ReflectRegistry.loadClassBySimpleName(model.getClass.getSimpleName)
      ).orElse(
        reflect.ReflectRegistry.getAllRegistered.find(_.simpleName == model.getClass.getSimpleName)
      ).getOrElse(
        throw new IllegalArgumentException(s"Cannot find type descriptor for $typeName")
      )
    }
    val value = SerializerFactory.buildFromType(typeDescriptor).asInstanceOf[Serializer[Any]]
    value.serialize(model, new JavaContext(typeDescriptor))
  }
}

class JsonMapper {
  def deserialize[M](json: Dynamic, meta: reflect.TypeDescriptor): M =
    JsonMapper.deserialize(json, meta)

  def deserializeArray[M](json: js.Array[js.Dynamic], meta: reflect.TypeDescriptor): Seq[M] =
    JsonMapper.deserializeArray(json, meta)

  def serialize(model: Any): Dynamic =
    JsonMapper.serialize(model)

  def serialize[M](model: M, meta: reflect.TypeDescriptor): Dynamic =
    JsonMapper.serialize(model, meta)
}
