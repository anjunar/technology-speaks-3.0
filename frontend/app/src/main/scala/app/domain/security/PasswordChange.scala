package app.domain.security

import app.support.JsonModel
import jfx.core.macros.typedProperty
import jfx.core.state.{Property, PropertyAccess}

import scala.scalajs.js

class PasswordChange(
  val currentPassword: Property[String] = Property(""),
  val newPassword: Property[String] = Property(""),
  val confirmPassword: Property[String] = Property("")
) extends JsonModel[PasswordChange] {

  override def properties: js.Array[PropertyAccess[PasswordChange, ?]] =
    PasswordChange.properties
}

object PasswordChange {
  val properties: js.Array[PropertyAccess[PasswordChange, ?]] = js.Array(
    typedProperty[PasswordChange, Property[String], String](_.currentPassword),
    typedProperty[PasswordChange, Property[String], String](_.newPassword),
    typedProperty[PasswordChange, Property[String], String](_.confirmPassword)
  )
}
