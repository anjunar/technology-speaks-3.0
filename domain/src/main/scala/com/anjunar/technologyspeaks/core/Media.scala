package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.schema.{DefaultWritableRule, EntitySchema, SchemaProvider}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.*

import scala.beans.BeanProperty
import scala.compiletime.uninitialized

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
class Media(name: String, contentType: String, data: Array[Byte])
  extends Thumbnail(name, contentType, data) {

  def this() = this(null, null, null)

  @OneToOne(cascade = Array(CascadeType.ALL))
  @JsonbProperty
  var thumbnail: Thumbnail = uninitialized

}

object Media extends SchemaProvider {

  override def schema(): EntitySchema[?] = new Schema

  class Schema extends AbstractEntitySchema[Media] {
    @JsonbProperty val name = property(_.name, new DefaultWritableRule[Media]())
    @JsonbProperty val contentType = property(_.contentType, new DefaultWritableRule[Media]())
    @JsonbProperty val data = property(_.data, new DefaultWritableRule[Media]())
    @JsonbProperty val thumbnail = property(_.thumbnail, new DefaultWritableRule[Media]())
  }

}
