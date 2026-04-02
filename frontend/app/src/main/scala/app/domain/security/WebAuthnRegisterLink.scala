package app.domain.security

import app.domain.core.{AbstractLink, UserInfo}
import jfx.core.meta.Meta
import jfx.json.JsonType

import scala.scalajs.js

@JsonType("web-authn-register-options")
class WebAuthnRegisterLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends AbstractLink {

  override def name: String = "Registrierung mit WebAuthn"

  override def icon: String = "how_to_reg"
}

