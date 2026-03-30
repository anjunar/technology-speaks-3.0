package app.domain.security

import app.domain.core.UserInfo
import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import jfx.core.state.Property

import scala.scalajs.js

class WebAuthnLogin(
  val email: Property[String] = Property("")
) extends JsonModel[WebAuthnLogin] {

  override def properties: Seq[PropertyAccess[WebAuthnLogin, ?]] = WebAuthnLogin.properties
}

object WebAuthnLogin {
  val properties: Seq[PropertyAccess[WebAuthnLogin, ?]] = PropertyMacros.describeProperties[WebAuthnLogin]
}
