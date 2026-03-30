package app.domain.security

import app.domain.core.AbstractLink
import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}

import scala.scalajs.js

class PasswordLoginLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[PasswordLoginLink] with AbstractLink {

  override def properties: Seq[PropertyAccess[PasswordLoginLink, ?]] = PasswordLoginLink.properties

  override def name: String = "Login mit Passwort"

  override def icon: String = "login"
}

object PasswordLoginLink {
  val properties: Seq[PropertyAccess[PasswordLoginLink, ?]]= PropertyMacros.describeProperties[PasswordLoginLink]}
