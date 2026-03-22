package app.domain.security

import app.support.JsonModel
import jfx.core.macros.property
import jfx.core.state.{Property, PropertyAccess}

import scala.scalajs.js

class WebAuthnRegister(
  var email: Property[String] = Property(""),
  var nickName: Property[String] = Property("")
) extends JsonModel[WebAuthnRegister] {

  override def properties: js.Array[PropertyAccess[WebAuthnRegister, ?]] =
    WebAuthnRegister.properties
}

object WebAuthnRegister {
  val properties: js.Array[PropertyAccess[WebAuthnRegister, ?]] = js.Array(
    property(_.email),
    property(_.nickName)
  )
}
