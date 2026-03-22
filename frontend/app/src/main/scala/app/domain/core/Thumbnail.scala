package app.domain.core

import jfx.core.macros.property
import jfx.core.state.{ListProperty, Property, PropertyAccess}

import scala.scalajs.js

class Thumbnail(
  var id: Property[String] = Property(""),
  var modified: Property[String] = Property(""),
  var created: Property[String] = Property(""),
  var name: Property[String] = Property(""),
  var contentType: Property[String] = Property(""),
  var data: Property[String] = Property(""),
  var links: ListProperty[Link] = ListProperty()
) extends AbstractEntity[Thumbnail] {

  override def properties: js.Array[PropertyAccess[Thumbnail, ?]] =
    Thumbnail.properties

  def dataUrl(): String =
    s"data:${contentType.get};base64,${data.get}"
}

object Thumbnail {
  val properties: js.Array[PropertyAccess[Thumbnail, ?]] = js.Array(
    property(_.id),
    property(_.modified),
    property(_.created),
    property(_.name),
    property(_.contentType),
    property(_.data),
    property(_.links)
  )
}
