package app.domain.security

import app.domain.core.AbstractLink
import app.support.JsonModel
import jfx.core.macros.property
import jfx.core.state.PropertyAccess

import scala.scalajs.js

class AccountLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[AccountLink] with AbstractLink {

  override def properties: js.Array[PropertyAccess[AccountLink, ?]] =
    AccountLink.properties

  override def name: String = "Account"

  override def icon: String = "manage_accounts"
}

object AccountLink {
  val properties: js.Array[PropertyAccess[AccountLink, ?]] = js.Array(
    property(_.rel),
    property(_.url),
    property(_.method)
  )
}
