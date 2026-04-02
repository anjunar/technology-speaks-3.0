package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.schema.{DefaultWritableRule, EntitySchema, SchemaProvider}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.*
import jakarta.validation.constraints.{NotBlank, NotNull, Size}

import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.compiletime.uninitialized
import com.anjunar.technologyspeaks.SpringContext
import com.anjunar.technologyspeaks.core.Media.Schema

@Entity
@NamedEntityGraph(
  name = "Media.full",
  attributeNodes = Array(
    new NamedAttributeNode("name"),
    new NamedAttributeNode("contentType"),
    new NamedAttributeNode("data"),
    new NamedAttributeNode("thumbnail")
  )
)
class Media(@(NotBlank @field)
            @(Size @field)(min = 2, max = 80)
            @(JsonbProperty @field)
            name: String,

            @(NotBlank @field)
            @(Size @field)(min = 2, max = 80)
            @(JsonbProperty @field)
            contentType: String,

            @(NotNull @field)
            @(JsonbProperty @field)
            data: Array[Byte])
  extends Thumbnail(name, contentType, data) {

  def this() = this(null, null, null)

  @OneToOne(cascade = Array(CascadeType.ALL))
  @JsonbProperty
  var thumbnail: Thumbnail = uninitialized

}

object Media extends SchemaProvider[Schema] {

  class Schema extends AbstractEntitySchema[Media](SpringContext.entityManager()) {
    @JsonbProperty val name = property(_.name, classOf[DefaultWritableRule[Media]])
    @JsonbProperty val contentType = property(_.contentType, classOf[DefaultWritableRule[Media]])
    @JsonbProperty val data = property(_.data, classOf[DefaultWritableRule[Media]])
    @JsonbProperty val thumbnail = property(_.thumbnail, classOf[DefaultWritableRule[Media]])
  }

}
