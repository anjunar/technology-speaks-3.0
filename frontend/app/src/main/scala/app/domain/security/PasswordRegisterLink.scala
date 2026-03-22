package app.domain.security

import app.domain.core.AbstractLink
import app.support.JsonModel
import jfx.core.macros.property
import jfx.core.state.PropertyAccess

import scala.scalajs.js

class PasswordRegisterLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[PasswordRegisterLink] with AbstractLink {

  override def properties: js.Array[PropertyAccess[PasswordRegisterLink, ?]] =
    PasswordRegisterLink.properties

  override def name: String = "Registrierung mit Passwort"

  override def icon: String = "app_registration"
}

object PasswordRegisterLink {
  val properties: js.Array[PropertyAccess[PasswordRegisterLink, ?]] = js.Array(
    property(_.rel),
    property(_.url),
    property(_.method)
  )
}
