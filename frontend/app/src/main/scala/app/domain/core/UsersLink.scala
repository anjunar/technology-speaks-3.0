package app.domain.core

import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}

import scala.scalajs.js

class UsersLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[UsersLink] with AbstractLink {

  override def properties: Seq[PropertyAccess[UsersLink, ?]] = UsersLink.properties

  override def name: String = "Benutzer"

  override def icon: String = "diversity_3"
}

object UsersLink {
  val properties: Seq[PropertyAccess[UsersLink, ?]]= PropertyMacros.describeProperties[UsersLink]}
