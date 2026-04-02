package jfx.domain

import jfx.core.state.Property
import jfx.form.Model

import java.util.UUID
import scala.scalajs.js

class Thumbnail(
    val id: Property[UUID] = Property(UUID.randomUUID()),
    val name: Property[String] = Property(""),
    val contentType: Property[String] = Property(""),
    val data: Property[String] = Property("")
) extends Model[Thumbnail]

object Thumbnail {

}
