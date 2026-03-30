package app.domain.security

import app.domain.core.AbstractLink
import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}

import scala.scalajs.js

class WebAuthnLoginLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[WebAuthnLoginLink] with AbstractLink {

  override def properties: Seq[PropertyAccess[WebAuthnLoginLink, ?]] = WebAuthnLoginLink.properties

  override def name: String = "Login mit WebAuthn"

  override def icon: String = "fingerprint"
}

object WebAuthnLoginLink {
  val properties: Seq[PropertyAccess[WebAuthnLoginLink, ?]] = PropertyMacros.describeProperties[WebAuthnLoginLink]
}
