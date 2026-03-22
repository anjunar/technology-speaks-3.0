package app.domain.core

import jfx.core.macros.property
import jfx.core.state.{ListProperty, Property, PropertyAccess}

import java.util.UUID
import scala.scalajs.js

class Media(
             var id: Property[UUID] = Property(null),
             var modified: Property[String] = Property(""),
             var created: Property[String] = Property(""),
             var name: Property[String] = Property(""),
             var contentType: Property[String] = Property(""),
             var data: Property[String] = Property(""),
             var thumbnail: Property[Thumbnail | Null] = Property(null),
             var links: ListProperty[Link] = ListProperty()
) extends AbstractEntity[Media] {

  override def properties: js.Array[PropertyAccess[Media, ?]] =
    Media.properties

  def dataUrl(): String =
    s"data:${contentType.get};base64,${data.get}"

  def mediaLink(): String =
    s"/service/core/media/${id.get}"

  def thumbnailLink(): String =
    s"/service/core/media/${id.get}/thumbnail"
}

object Media {
  val properties: js.Array[PropertyAccess[Media, ?]] = js.Array(
    property(_.id),
    property(_.modified),
    property(_.created),
    property(_.name),
    property(_.contentType),
    property(_.data),
    property(_.thumbnail),
    property(_.links)
  )
}
