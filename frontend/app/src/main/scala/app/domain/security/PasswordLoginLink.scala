package app.domain.security

import app.domain.core.AbstractLink
import jfx.core.meta.Meta
import jfx.json.JsonType

import scala.scalajs.js

@JsonType("password-login-login")
class PasswordLoginLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends AbstractLink {

  override def name: String = "Login mit Passwort"

  override def icon: String = "login"
}
