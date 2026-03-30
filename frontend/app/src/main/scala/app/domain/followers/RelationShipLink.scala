package app.domain.followers

import app.domain.core.AbstractLink
import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}

import scala.scalajs.js

class RelationShipLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[RelationShipLink] with AbstractLink {

  override def properties: Seq[PropertyAccess[RelationShipLink, ?]] = RelationShipLink.properties

  override def name: String = "Followers"

  override def icon: String = "1k_plus"
}

object RelationShipLink {
  val properties: Seq[PropertyAccess[RelationShipLink, ?]]= PropertyMacros.describeProperties[RelationShipLink]}
