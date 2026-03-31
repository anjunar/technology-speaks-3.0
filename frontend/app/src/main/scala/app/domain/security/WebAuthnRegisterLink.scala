package app.domain.security

import app.domain.core.{AbstractLink, UserInfo}
import app.support.JsonModel
import jfx.core.meta.Meta

import scala.scalajs.js

class WebAuthnRegisterLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[WebAuthnRegisterLink] with AbstractLink {

  override def meta: Meta[WebAuthnRegisterLink] = WebAuthnRegisterLink.meta

  override def name: String = "Registrierung mit WebAuthn"

  override def icon: String = "how_to_reg"
}

object WebAuthnRegisterLink {
  val meta: Meta[WebAuthnRegisterLink] = Meta(() => new WebAuthnRegisterLink())
}
