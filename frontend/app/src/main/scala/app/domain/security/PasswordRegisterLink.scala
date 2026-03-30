package app.domain.security

import app.domain.core.AbstractLink
import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}

import scala.scalajs.js

class PasswordRegisterLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[PasswordRegisterLink] with AbstractLink {

  override def properties: Seq[PropertyAccess[PasswordRegisterLink, ?]] = PasswordRegisterLink.properties

  override def name: String = "Registrierung mit Passwort"

  override def icon: String = "app_registration"
}

object PasswordRegisterLink {
  val properties: Seq[PropertyAccess[PasswordRegisterLink, ?]] = PropertyMacros.describeProperties[PasswordRegisterLink]
}
