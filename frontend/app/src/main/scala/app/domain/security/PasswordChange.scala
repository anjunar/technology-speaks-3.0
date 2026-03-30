package app.domain.security

import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.{PropertyMacros, PropertyAccess}
import jfx.core.state.Property

import scala.scalajs.js

class PasswordChange(
  val currentPassword: Property[String] = Property(""),
  val newPassword: Property[String] = Property(""),
  val confirmPassword: Property[String] = Property("")
) extends JsonModel[PasswordChange] {

  override def properties: Seq[PropertyAccess[PasswordChange, ?]] = PasswordChange.properties
}

object PasswordChange {
  val properties: Seq[PropertyAccess[PasswordChange, ?]]= PropertyMacros.describeProperties[PasswordChange]}
