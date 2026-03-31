package app.domain.core

import app.support.JsonModel
import jfx.core.meta.Meta
import jfx.json.{JsonIgnore, JsonType}

import scala.scalajs.js

class Schema(
  var entries: js.Dictionary[SchemaProperty] = js.Dictionary()
) extends JsonModel[Schema] {

  override def meta: Meta[Schema] = Schema.meta

  def findProperty(name: String): SchemaProperty | Null =
    entries.get(name).orNull
}

object Schema {
  val meta: Meta[Schema] = Meta(() => new Schema())
}

@JsonType("Property")
class SchemaProperty(
  var name: String = "",
  var `type`: String = "",
  var schema: Schema | Null = null,
  @JsonIgnore
  var links: js.Array[Link] = js.Array()
) extends JsonModel[SchemaProperty] {

  override def meta: Meta[SchemaProperty] = SchemaProperty.meta
}

object SchemaProperty {
  val meta: Meta[SchemaProperty] = Meta(() => new SchemaProperty())
}
