package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.schema.{DefaultWritableRule, EntitySchema, SchemaProvider}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.{CascadeType, Entity, NamedAttributeNode, NamedEntityGraph, OneToOne}

import scala.beans.BeanProperty

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
class Media(
  name: String = null,
  contentType: String = null,
  data: Array[Byte] = null
) extends Thumbnail(name, contentType, data) {

  @OneToOne(cascade = Array(CascadeType.ALL))
  @JsonbProperty
  @BeanProperty
  var thumbnail: Thumbnail = null

}

object Media extends SchemaProvider {

  override def schema(): EntitySchema[?] = new Schema

  class Schema extends AbstractEntitySchema[Media] {
    @JsonbProperty val name = property[String]("name", classOf[String], new DefaultWritableRule[Media]())
    @JsonbProperty val contentType = property[String]("contentType", classOf[String], new DefaultWritableRule[Media]())
    @JsonbProperty val data = property[Array[Byte]]("data", classOf[Array[Byte]], new DefaultWritableRule[Media]())
    @JsonbProperty val thumbnail = property[Thumbnail]("thumbnail", classOf[Thumbnail], new DefaultWritableRule[Media]())
  }

}
