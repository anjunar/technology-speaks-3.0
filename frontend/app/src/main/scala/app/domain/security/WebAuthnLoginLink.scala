package app.domain.security

import app.domain.core.AbstractLink
import app.support.JsonModel
import jfx.core.meta.Meta
import jfx.json.JsonType

import scala.scalajs.js

@JsonType("web-authn-login-options")
class WebAuthnLoginLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[WebAuthnLoginLink] with AbstractLink {

  override def meta: Meta[WebAuthnLoginLink] = WebAuthnLoginLink.meta

  override def name: String = "Login mit WebAuthn"

  override def icon: String = "fingerprint"
}

object WebAuthnLoginLink {
  val meta: Meta[WebAuthnLoginLink] = Meta(() => new WebAuthnLoginLink())
}
