package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.schema.{DefaultWritableRule, EntitySchema, SchemaProvider}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.*
import jakarta.validation.constraints.{NotBlank, Size}

import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.compiletime.uninitialized
import com.anjunar.technologyspeaks.SpringContext
import com.anjunar.technologyspeaks.core.Thumbnail.Schema

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

                @(NotBlank @field) @(Size @field)(min = 2, max = 80)
                @(JsonbProperty @field)
                var contentType: String,

                @(Lob @field) 
                @(JsonbProperty @field) var data: Array[Byte])
  extends AbstractEntity {

  def this() = this(null, null, null)
  
}

object Thumbnail extends SchemaProvider[Schema] {

  class Schema extends AbstractEntitySchema[Thumbnail](SpringContext.entityManager()) {
    val name = property(_.name, classOf[DefaultWritableRule[Thumbnail]])
    val contentType = property(_.contentType, classOf[DefaultWritableRule[Thumbnail]])
    val data = property(_.data, classOf[DefaultWritableRule[Thumbnail]])
  }

}
