package app.domain.security

import app.domain.core.AbstractLink
import app.support.JsonModel
import jfx.core.meta.Meta
import jfx.json.JsonType

import scala.scalajs.js

@JsonType("password-register-register")
class PasswordRegisterLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[PasswordRegisterLink] with AbstractLink {



  override def name: String = "Registrierung mit Passwort"

  override def icon: String = "app_registration"
}

