package app.domain.security

import app.support.JsonModel
import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{Property, PropertyAccess}

import scala.scalajs.js

class WebAuthnRegister(
  val email: Property[String] = Property(""),
  val nickName: Property[String] = Property("")
) extends JsonModel[WebAuthnRegister] {

  override def properties: js.Array[PropertyAccess[WebAuthnRegister, ?]] =
    WebAuthnRegister.properties
}

object WebAuthnRegister {
  val properties: js.Array[PropertyAccess[WebAuthnRegister, ?]] = js.Array(
    typedProperty[WebAuthnRegister, Property[String], String](_.email),
    typedProperty[WebAuthnRegister, Property[String], String](_.nickName)
  )
}
