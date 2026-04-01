package app.domain.security

import app.support.JsonModel
import jfx.core.meta.Meta
import jfx.core.state.Property

import scala.scalajs.js

class CreatePassword(
  val newPassword: Property[String] = Property(""),
  val confirmPassword: Property[String] = Property("")
) extends JsonModel[CreatePassword] {

  override def meta: Meta[CreatePassword] = CreatePassword.meta
}

object CreatePassword {
  val meta : Meta[CreatePassword] = Meta(() => new CreatePassword())}
