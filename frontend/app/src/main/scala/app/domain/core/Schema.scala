package app.domain.core

import app.support.JsonModel
import jfx.core.macros.property
import jfx.core.state.PropertyAccess

import scala.scalajs.js

class Schema(
  var entries: js.Dictionary[SchemaProperty] = js.Dictionary()
) extends JsonModel[Schema] {

  override def properties: js.Array[PropertyAccess[Schema, ?]] =
    Schema.properties

  def findProperty(name: String): SchemaProperty | Null =
    entries.get(name).orNull
}

object Schema {
  val properties: js.Array[PropertyAccess[Schema, ?]] = js.Array(
    property(_.entries)
  )
}

class SchemaProperty(
  var name: String = "",
  var `type`: String = "",
  var schema: Schema | Null = null,
  var links: js.Array[Link] = js.Array()
) extends JsonModel[SchemaProperty] {

  override def properties: js.Array[PropertyAccess[SchemaProperty, ?]] =
    SchemaProperty.properties
}

object SchemaProperty {
  val properties: js.Array[PropertyAccess[SchemaProperty, ?]] = js.Array(
    property(_.name),
    property(_.`type`),
    property(_.schema),
    property(_.links)
  )
}
