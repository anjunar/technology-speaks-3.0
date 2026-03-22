package app.domain.security

import app.support.JsonModel
import jfx.core.macros.property
import jfx.core.state.{Property, PropertyAccess}

import scala.scalajs.js

class WebAuthnLogin(
  var email: Property[String] = Property("")
) extends JsonModel[WebAuthnLogin] {

  override def properties: js.Array[PropertyAccess[WebAuthnLogin, ?]] =
    WebAuthnLogin.properties
}

object WebAuthnLogin {
  val properties: js.Array[PropertyAccess[WebAuthnLogin, ?]] = js.Array(
    property(_.email)
  )
}
