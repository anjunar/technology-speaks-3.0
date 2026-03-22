package app.domain.security

import app.domain.core.AbstractLink
import app.support.JsonModel
import jfx.core.macros.property
import jfx.core.state.PropertyAccess

import scala.scalajs.js

class LogoutLink(
  var id: String = "logout-logout",
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[LogoutLink] with AbstractLink {

  override def properties: js.Array[PropertyAccess[LogoutLink, ?]] =
    LogoutLink.properties

  override def name: String = "Logout"

  override def icon: String = "logout"
}

object LogoutLink {
  val properties: js.Array[PropertyAccess[LogoutLink, ?]] = js.Array(
    property(_.id),
    property(_.rel),
    property(_.url),
    property(_.method)
  )
}
