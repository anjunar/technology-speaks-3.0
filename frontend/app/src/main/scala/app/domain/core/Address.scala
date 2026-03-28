package app.domain.core

import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{ListProperty, Property, PropertyAccess}
import jfx.form.validators.{NotBlankValidator, SizeValidator}

import java.util.UUID
import scala.scalajs.js

class Address extends AbstractEntity[Address] {

  val street: Property[String] = Property("")
  val number: Property[String] = Property("")
  val zipCode: Property[String] = Property("")
  val country: Property[String] = Property("")

  override def properties: js.Array[PropertyAccess[Address, ?]] =
    Address.properties
}

object Address {
  val properties: js.Array[PropertyAccess[Address, ?]] = js.Array(
    typedProperty[Address, Property[UUID], UUID](_.id),
    typedProperty[Address, Property[String], String](_.modified),
    typedProperty[Address, Property[String], String](_.created),
    typedProperty[Address, Property[String], String](_.street)
      .withValidator(NotBlankValidator())
      .withValidator(SizeValidator(2, 80)),
    typedProperty[Address, Property[String], String](_.number)
      .withValidator(NotBlankValidator())
      .withValidator(SizeValidator(1, 80)),
    typedProperty[Address, Property[String], String](_.zipCode)
      .withValidator(NotBlankValidator())
      .withValidator(SizeValidator(5, 5)),
    typedProperty[Address, Property[String], String](_.country)
      .withValidator(NotBlankValidator())
      .withValidator(SizeValidator(2, 80)),
    typedProperty[Address, ListProperty[Link], Link](_.links)
  )
}
