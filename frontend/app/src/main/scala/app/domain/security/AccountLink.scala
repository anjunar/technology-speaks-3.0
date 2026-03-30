package app.domain.security

import app.domain.core.AbstractLink
import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}

import scala.scalajs.js

class AccountLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[AccountLink] with AbstractLink {

  override def properties: Seq[PropertyAccess[AccountLink, ?]] = AccountLink.properties

  override def name: String = "Account"

  override def icon: String = "manage_accounts"
}

object AccountLink {
  val properties: Seq[PropertyAccess[AccountLink, ?]]= PropertyMacros.describeProperties[AccountLink]}
