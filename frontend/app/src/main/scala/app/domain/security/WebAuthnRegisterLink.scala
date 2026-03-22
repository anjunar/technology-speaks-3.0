package app.domain.security

import app.domain.core.AbstractLink
import app.support.JsonModel
import jfx.core.macros.property
import jfx.core.state.PropertyAccess

import scala.scalajs.js

class WebAuthnRegisterLink(
  var id: String = "web-authn-register-options",
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[WebAuthnRegisterLink] with AbstractLink {

  override def properties: js.Array[PropertyAccess[WebAuthnRegisterLink, ?]] =
    WebAuthnRegisterLink.properties

  override def name: String = "Registrierung mit WebAuthn"

  override def icon: String = "how_to_reg"
}

object WebAuthnRegisterLink {
  val properties: js.Array[PropertyAccess[WebAuthnRegisterLink, ?]] = js.Array(
    property(_.id),
    property(_.rel),
    property(_.url),
    property(_.method)
  )
}
