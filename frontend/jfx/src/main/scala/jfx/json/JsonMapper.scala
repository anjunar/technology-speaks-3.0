package jfx.json

import jfx.json.deserializer.{Deserializer, DeserializerFactory, JsonContext, ModelDeserializer}
import jfx.json.serializer.{JavaContext, ModelSerializer}
import jfx.form.Model

import scala.scalajs.js
import scala.scalajs.js.Dynamic

object JsonMapper {
  def deserialize[M <: Model[M]](json: Dynamic, meta: com.anjunar.scala.enterprise.macros.reflection.Type): M =
    new ModelDeserializer().deserialize(json, new JsonContext(meta)).asInstanceOf[M]

  def deserializeArray[M <: Model[M]](json: js.Array[js.Dynamic], meta: com.anjunar.scala.enterprise.macros.reflection.Type): Seq[M] =
    if (json == null || js.isUndefined(json)) Seq.empty
    else json.toSeq.map(j => deserialize(j, meta))

  def serialize(model: Model[?]): Dynamic =
    new ModelSerializer().serialize(model, new JavaContext(model.meta))
}

class JsonMapper {
  def deserialize[M <: Model[M]](json: Dynamic, meta: com.anjunar.scala.enterprise.macros.reflection.Type): M =
    JsonMapper.deserialize(json, meta)

  def deserializeArray[M <: Model[M]](json: js.Array[js.Dynamic], meta: com.anjunar.scala.enterprise.macros.reflection.Type): Seq[M] =
    JsonMapper.deserializeArray(json, meta)

  def serialize(model: Model[?]): Dynamic =
    JsonMapper.serialize(model)
}
