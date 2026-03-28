package app.domain.security

import app.support.JsonModel
import jfx.core.macros.typedProperty
import jfx.core.state.{Property, PropertyAccess}

import scala.scalajs.js

class CreatePassword(
  val newPassword: Property[String] = Property(""),
  val confirmPassword: Property[String] = Property("")
) extends JsonModel[CreatePassword] {

  override def properties: js.Array[PropertyAccess[CreatePassword, ?]] =
    CreatePassword.properties
}

object CreatePassword {
  val properties: js.Array[PropertyAccess[CreatePassword, ?]] = js.Array(
    typedProperty[CreatePassword, Property[String], String](_.newPassword),
    typedProperty[CreatePassword, Property[String], String](_.confirmPassword)
  )
}
