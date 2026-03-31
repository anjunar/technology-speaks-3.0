package app.domain.security

import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.{PropertyMacros, PropertyAccess}
import com.anjunar.scala.enterprise.macros.validation.{NotBlank, Size}
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

  override def properties: Seq[PropertyAccess[PasswordChange, ?]] = PasswordChange.properties
}

object PasswordChange {
  val properties: Seq[PropertyAccess[PasswordChange, ?]]= PropertyMacros.describeProperties[PasswordChange]}
