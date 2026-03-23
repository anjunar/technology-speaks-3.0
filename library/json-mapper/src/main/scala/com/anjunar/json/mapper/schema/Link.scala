package com.anjunar.json.mapper.schema

import jakarta.json.bind.annotation.JsonbProperty

import scala.annotation.meta.field
import scala.beans.BeanProperty

class Link(
  @(JsonbProperty @field) val rel: String = null,
  @(JsonbProperty @field) val url: String = null,
  @(JsonbProperty @field) val method: String = null,
  @(JsonbProperty @field)("@type") val id: String = null
)

object Link extends SchemaProvider {

  override def schema(): EntitySchema[?] = new Schema

  class Schema extends EntitySchema[Link] {
    @JsonbProperty("@type") val id = property("id", classOf[String])
    @JsonbProperty val rel = property("rel", classOf[String])
    @JsonbProperty val url = property("url", classOf[String])
    @JsonbProperty val method = property("method", classOf[String])
  }

}
