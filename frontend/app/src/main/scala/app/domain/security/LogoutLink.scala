package app.domain.security

import app.domain.core.AbstractLink
import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}

import scala.scalajs.js

class LogoutLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[LogoutLink] with AbstractLink {

  override def properties: Seq[PropertyAccess[LogoutLink, ?]] = LogoutLink.properties

  override def name: String = "Logout"

  override def icon: String = "logout"
}

object LogoutLink {
  val properties: Seq[PropertyAccess[LogoutLink, ?]]= PropertyMacros.describeProperties[LogoutLink]}
