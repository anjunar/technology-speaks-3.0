package com.anjunar.json.mapper.schema

import jakarta.json.bind.annotation.JsonbProperty

import scala.annotation.meta.field
import scala.beans.BeanProperty

class Link(
  @(JsonbProperty @field) @BeanProperty val rel: String = null,
  @(JsonbProperty @field) @BeanProperty val url: String = null,
  @(JsonbProperty @field) @BeanProperty val method: String = null,
  @(JsonbProperty @field)("@type") @BeanProperty val id: String = null
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
