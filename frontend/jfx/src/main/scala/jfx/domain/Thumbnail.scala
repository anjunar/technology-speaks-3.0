package jfx.domain

import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{Property, PropertyAccess}
import jfx.form.Model
import jfx.form.validators.{NotBlankValidator, SizeValidator}

import java.util.UUID
import scala.scalajs.js

class Thumbnail(
    val id : Property[UUID] = Property(null),               
    val name: Property[String] = Property(""),
    val contentType: Property[String] = Property(""),
    val data: Property[String] = Property("")
) extends Model[Thumbnail] {

  override def properties: js.Array[PropertyAccess[Thumbnail, ?]] = Thumbnail.properties
}

object Thumbnail {
  val properties: js.Array[PropertyAccess[Thumbnail, ?]] = js.Array(
    typedProperty[Thumbnail, Property[UUID], UUID](_.id),
    typedProperty[Thumbnail, Property[String], String](_.name)
      .withValidator(NotBlankValidator())
      .withValidator(SizeValidator(2, 80)),
    typedProperty[Thumbnail, Property[String], String](_.contentType)
      .withValidator(NotBlankValidator())
      .withValidator(SizeValidator(2, 80)),
    typedProperty[Thumbnail, Property[String], String](_.data)
  )
}
