package app.domain.security

import app.domain.core.AbstractLink
import app.support.JsonModel
import jfx.core.macros.property
import jfx.core.state.PropertyAccess

import scala.scalajs.js

class PasswordLoginLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[PasswordLoginLink] with AbstractLink {

  override def properties: js.Array[PropertyAccess[PasswordLoginLink, ?]] =
    PasswordLoginLink.properties

  override def name: String = "Login mit Passwort"

  override def icon: String = "login"
}

object PasswordLoginLink {
  val properties: js.Array[PropertyAccess[PasswordLoginLink, ?]] = js.Array(
    property(_.rel),
    property(_.url),
    property(_.method)
  )
}
