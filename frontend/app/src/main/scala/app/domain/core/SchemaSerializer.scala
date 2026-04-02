package app.domain.core

import jfx.json.serializer.{JavaContext, ModelSerializer, Serializer}
import jfx.form.Model

import scala.scalajs.js
import scala.scalajs.js.Dynamic

class SchemaSerializer extends Serializer[Schema] {

  override def serialize(input: Schema, context: JavaContext): Dynamic = {
    val out = js.Dictionary[js.Any]()
    out.update("@type", "Schema")

    input.entries.foreach { case (key, value) =>
      val serializer = new SchemaPropertySerializer()
      val serialized = serializer.serialize(value, new JavaContext(null))
      out.update(key, serialized)
    }

    out.asInstanceOf[js.Dynamic]
  }

}

class SchemaPropertySerializer extends Serializer[SchemaProperty] {

  override def serialize(input: SchemaProperty, context: JavaContext): Dynamic = {
    val out = js.Dictionary[js.Any]()
    out.update("@type", "Property")

    if (input.name != null) out.update("name", input.name)
    if (input.`type` != null) out.update("type", input.`type`)
    if (input.schema != null) {
      val schemaSerializer = new SchemaSerializer()
      out.update("schema", schemaSerializer.serialize(input.schema, new JavaContext(null)))
    }

    out.asInstanceOf[js.Dynamic]
  }

}
