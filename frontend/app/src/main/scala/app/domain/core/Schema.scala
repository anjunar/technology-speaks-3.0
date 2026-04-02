package app.domain.core

import jfx.core.meta.Meta
import jfx.json.{JsonIgnore, JsonName, JsonType}

import scala.annotation.meta.field
import scala.scalajs.js

@JsonType("Schema")
class Schema(
  var entries: Map[String, SchemaProperty] = Map.empty
) {

  def findProperty(name: String): SchemaProperty | Null =
    entries.get(name).orNull
}

@JsonType("Property")
class SchemaProperty(
  var name: String = "",
  var `type`: String = "",
  var schema: Schema | Null = null,
  @(JsonName @field)("$links")
  var links: js.Array[Link] = js.Array()
) 