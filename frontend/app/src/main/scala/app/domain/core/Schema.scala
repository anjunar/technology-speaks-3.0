package app.domain.core

import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import jfx.json.JsonIgnore

import scala.scalajs.js

class Schema(
  var entries: js.Dictionary[SchemaProperty] = js.Dictionary()
) extends JsonModel[Schema] {

  override def properties: Seq[PropertyAccess[Schema, ?]] = Schema.properties

  def findProperty(name: String): SchemaProperty | Null =
    entries.get(name).orNull
}

object Schema {
  val properties: Seq[PropertyAccess[Schema, ?]] = PropertyMacros.describeProperties[Schema]
}

class SchemaProperty(
  var name: String = "",
  var `type`: String = "",
  @JsonIgnore
  var schema: Schema | Null = null,
  @JsonIgnore
  var links: js.Array[Link] = js.Array()
) extends JsonModel[SchemaProperty] {

  override def properties: Seq[PropertyAccess[SchemaProperty, ?]] = SchemaProperty.properties
}

object SchemaProperty {
  val properties: Seq[PropertyAccess[SchemaProperty, ?]] = PropertyMacros.describeProperties[SchemaProperty]
}
