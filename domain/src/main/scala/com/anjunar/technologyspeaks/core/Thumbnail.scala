package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.schema.{DefaultWritableRule, EntitySchema, SchemaProvider}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.*
import jakarta.validation.constraints.{NotBlank, Size}

import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.compiletime.uninitialized

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
class Thumbnail(@(NotBlank @field) 
                @(Size @field)(min = 2, max = 80)
                @(JsonbProperty @field)
                var name: String,

                @NotBlank @Size(min = 2, max = 80)
                @(JsonbProperty @field)
                var contentType: String,

                @Lob 
                @(JsonbProperty @field) var data: Array[Byte])
  extends AbstractEntity {

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
