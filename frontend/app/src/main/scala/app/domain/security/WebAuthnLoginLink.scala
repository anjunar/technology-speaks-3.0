package app.domain.security

import app.domain.core.AbstractLink
import app.support.JsonModel
import jfx.core.macros.property
import jfx.core.state.PropertyAccess

import scala.scalajs.js

class WebAuthnLoginLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[WebAuthnLoginLink] with AbstractLink {

  override def properties: js.Array[PropertyAccess[WebAuthnLoginLink, ?]] =
    WebAuthnLoginLink.properties

  override def name: String = "Login mit WebAuthn"

  override def icon: String = "fingerprint"
}

object WebAuthnLoginLink {
  val properties: js.Array[PropertyAccess[WebAuthnLoginLink, ?]] = js.Array(
    property(_.rel),
    property(_.url),
    property(_.method)
  )
}
