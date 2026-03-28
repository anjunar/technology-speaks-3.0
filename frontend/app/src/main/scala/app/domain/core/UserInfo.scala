package app.domain.core

import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{ListProperty, Property, PropertyAccess}
import jfx.form.validators.{NotBlankValidator, NotNullValidator, SizeValidator}

import java.util.UUID
import scala.scalajs.js

class UserInfo extends AbstractEntity[UserInfo] {

  val firstName: Property[String] = Property("")
  val lastName: Property[String] = Property("")
  val birthDate: Property[String] = Property("")

  override def properties: js.Array[PropertyAccess[UserInfo, ?]] =
    UserInfo.properties
}

object UserInfo {
  val properties: js.Array[PropertyAccess[UserInfo, ?]] = js.Array(
    typedProperty[UserInfo, Property[UUID], UUID](_.id),
    typedProperty[UserInfo, Property[String], String](_.modified),
    typedProperty[UserInfo, Property[String], String](_.created),
    typedProperty[UserInfo, Property[String], String](_.firstName)
      .withValidator(NotBlankValidator())
      .withValidator(SizeValidator(2, 80)),
    typedProperty[UserInfo, Property[String], String](_.lastName)
      .withValidator(NotBlankValidator())
      .withValidator(SizeValidator(2, 80)),
    typedProperty[UserInfo, Property[String], String](_.birthDate)
      .withValidator(NotNullValidator()),
    typedProperty[UserInfo, ListProperty[Link], Link](_.links)
  )
}
