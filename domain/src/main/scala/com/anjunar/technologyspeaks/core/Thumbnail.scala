package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.schema.{DefaultWritableRule, EntitySchema, SchemaProvider}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.{Entity, Lob, NamedAttributeNode, NamedEntityGraph, Table}
import jakarta.validation.constraints.{NotBlank, Size}

import scala.annotation.meta.field
import scala.beans.BeanProperty

@Entity
@Table(name = "Core#Media")
@NamedEntityGraph(
  name = "Thumbnail.full",
  attributeNodes = Array(
    new NamedAttributeNode("name"),
    new NamedAttributeNode("contentType"),
    new NamedAttributeNode("data")
  )
)
class Thumbnail(
  @NotBlank @Size(min = 2, max = 80) @(JsonbProperty @field) @BeanProperty var name: String = null,
  @NotBlank @Size(min = 2, max = 80) @(JsonbProperty @field) @BeanProperty var contentType: String = null,
  @Lob @(JsonbProperty @field) @BeanProperty var data: Array[Byte] = null
) extends AbstractEntity {
  def this() = this(null, null, null)
}

object Thumbnail extends SchemaProvider {

  override def schema(): EntitySchema[?] = new Schema

  class Schema extends AbstractEntitySchema[Thumbnail] {
    val name = property[String]("name", classOf[String], new DefaultWritableRule[Thumbnail]())
    val contentType = property[String]("contentType", classOf[String], new DefaultWritableRule[Thumbnail]())
    val data = property[Array[Byte]]("data", classOf[Array[Byte]], new DefaultWritableRule[Thumbnail]())
  }

}
