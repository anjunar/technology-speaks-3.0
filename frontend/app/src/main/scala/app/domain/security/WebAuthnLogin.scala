package app.domain.security

import app.support.JsonModel
import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{Property, PropertyAccess}
import jfx.form.validators.{EmailValidator, NotBlankValidator}

import scala.scalajs.js

class WebAuthnLogin(
  val email: Property[String] = Property("")
) extends JsonModel[WebAuthnLogin] {

  override def properties: js.Array[PropertyAccess[WebAuthnLogin, ?]] =
    WebAuthnLogin.properties
}

object WebAuthnLogin {
  val properties: js.Array[PropertyAccess[WebAuthnLogin, ?]] = js.Array(
    typedProperty[WebAuthnLogin, Property[String], String](_.email)
      .withValidator(NotBlankValidator())
      .withValidator(EmailValidator())
  )
}
