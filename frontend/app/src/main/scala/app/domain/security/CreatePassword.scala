package app.domain.security

import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.{PropertyMacros, PropertyAccess}
import jfx.core.state.Property

import scala.scalajs.js

class CreatePassword(
  val newPassword: Property[String] = Property(""),
  val confirmPassword: Property[String] = Property("")
) extends JsonModel[CreatePassword] {

  override def properties: Seq[PropertyAccess[CreatePassword, ?]] = CreatePassword.properties
}

object CreatePassword {
  val properties: Seq[PropertyAccess[CreatePassword, ?]]= PropertyMacros.describeProperties[CreatePassword]}
