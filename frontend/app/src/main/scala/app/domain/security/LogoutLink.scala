package app.domain.security

import app.domain.core.AbstractLink
import app.support.JsonModel
import jfx.core.meta.Meta
import jfx.json.JsonType

import scala.scalajs.js

@JsonType("logout-logout")
class LogoutLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[LogoutLink] with AbstractLink {



  override def name: String = "Logout"

  override def icon: String = "logout"
}
