package app.domain.core

import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.validation.JsonName
import jfx.core.meta.Meta
import jfx.json.{JsonIgnore, JsonType}

import scala.annotation.meta.field
import scala.scalajs.js

@JsonType("Schema")
class Schema(
  var entries: Map[String, SchemaProperty] = Map.empty
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
  @(JsonName @field)("$links")
  var links: js.Array[Link] = js.Array()
) extends JsonModel[SchemaProperty] {

  override def meta: Meta[SchemaProperty] = SchemaProperty.meta
}

object SchemaProperty {
  val meta: Meta[SchemaProperty] = Meta(() => new SchemaProperty())
}
