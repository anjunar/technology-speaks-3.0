package jfx.domain

import jfx.core.state.Property
import jfx.form.Model

import java.util.UUID
import scala.scalajs.js

class Media(
    val id: Property[UUID] = Property(UUID.randomUUID()),
    val thumbnail: Property[Thumbnail | Null] = Property(null),
    val name: Property[String] = Property(""),
    val contentType: Property[String] = Property(""),
    val data: Property[String] = Property("")
) extends Model[Media]

object Media {

}
