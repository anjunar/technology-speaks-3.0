package app.domain.security

import app.domain.core.UserInfo
import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import jfx.core.state.Property
import jfx.form.validators.{EmailValidator, NotBlankValidator, SizeValidator}

import scala.scalajs.js

class WebAuthnRegister(
  val email: Property[String] = Property(""),
  val nickName: Property[String] = Property("")
) extends JsonModel[WebAuthnRegister] {

  override def properties: Seq[PropertyAccess[WebAuthnRegister, ?]] = WebAuthnRegister.properties
}

object WebAuthnRegister {
  val properties: Seq[PropertyAccess[WebAuthnRegister, ?]] = PropertyMacros.describeProperties[WebAuthnRegister]
}
