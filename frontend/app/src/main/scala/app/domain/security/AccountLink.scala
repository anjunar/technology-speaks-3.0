package app.domain.security

import app.domain.core.AbstractLink
import jfx.core.meta.Meta
import jfx.json.JsonType

import scala.scalajs.js

class AccountLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends AbstractLink {

  override def name: String = "Account"

  override def icon: String = "manage_accounts"
}
