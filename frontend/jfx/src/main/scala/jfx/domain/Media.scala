package jfx.domain

import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{Property, PropertyAccess}

import java.util.UUID
import scala.scalajs.js

class Media(
    id : Property[UUID] = Property(null),
    name: Property[String] = Property(""),
    contentType: Property[String] = Property(""),
    data: Property[String] = Property(""),
    var thumbnail: Property[Thumbnail] = Property[Thumbnail](null)
) extends Thumbnail(id, name, contentType, data) {

  override def properties: js.Array[PropertyAccess[Thumbnail, ?]] = Media.properties.asInstanceOf[js.Array[PropertyAccess[Thumbnail, ?]]]
}

object Media {
  val properties: js.Array[PropertyAccess[Media, ?]] = js.Array(
    typedProperty[Media, Property[UUID], UUID](_.id),
    typedProperty[Media, Property[String], String](_.name),
    typedProperty[Media, Property[String], String](_.contentType),
    typedProperty[Media, Property[String], String](_.data),
    typedProperty[Media, Property[Thumbnail], Thumbnail](_.thumbnail)
  )
}
