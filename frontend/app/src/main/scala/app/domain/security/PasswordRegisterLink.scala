package app.domain.security

import app.domain.core.AbstractLink
import app.support.JsonModel
import jfx.core.meta.Meta

import scala.scalajs.js

class PasswordRegisterLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[PasswordRegisterLink] with AbstractLink {

  override def meta: Meta[PasswordRegisterLink] = PasswordRegisterLink.meta

  override def name: String = "Registrierung mit Passwort"

  override def icon: String = "app_registration"
}

object PasswordRegisterLink {
  val meta: Meta[PasswordRegisterLink] = Meta(() => new PasswordRegisterLink())
}
