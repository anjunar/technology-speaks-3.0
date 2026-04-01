package app.domain.security

import app.domain.core.AbstractLink
import app.support.JsonModel
import jfx.core.meta.Meta

import scala.scalajs.js

class AccountLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[AccountLink] with AbstractLink {

  override def meta: Meta[AccountLink] = AccountLink.meta

  override def name: String = "Account"

  override def icon: String = "manage_accounts"
}

object AccountLink {
  val meta : Meta[AccountLink] = Meta(() => new AccountLink())}
