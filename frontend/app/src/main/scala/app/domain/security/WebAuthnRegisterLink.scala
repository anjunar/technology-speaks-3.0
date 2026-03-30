package app.domain.security

import app.domain.core.{AbstractLink, UserInfo}
import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}

import scala.scalajs.js

class WebAuthnRegisterLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[WebAuthnRegisterLink] with AbstractLink {

  override def properties: Seq[PropertyAccess[WebAuthnRegisterLink, ?]] = WebAuthnRegisterLink.properties

  override def name: String = "Registrierung mit WebAuthn"

  override def icon: String = "how_to_reg"
}

object WebAuthnRegisterLink {
  val properties: Seq[PropertyAccess[WebAuthnRegisterLink, ?]] = PropertyMacros.describeProperties[WebAuthnRegisterLink]
}
