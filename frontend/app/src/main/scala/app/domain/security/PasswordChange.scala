package app.domain.security

import app.support.JsonModel
import jfx.core.meta.Meta
import jfx.form.validators.{NotBlank, Size}
import jfx.core.state.Property

import scala.scalajs.js

class PasswordChange(
  @NotBlank(message = "Aktuelles Passwort ist erforderlich")
  val currentPassword: Property[String] = Property(""),

  @NotBlank(message = "Neues Passwort ist erforderlich")
  @Size(min = 8, max = 128, message = "Neues Passwort muss zwischen 8 und 128 Zeichen haben")
  val newPassword: Property[String] = Property(""),

  @NotBlank(message = "Passwortbestätigung ist erforderlich")
  val confirmPassword: Property[String] = Property("")
) extends JsonModel[PasswordChange] {


}

