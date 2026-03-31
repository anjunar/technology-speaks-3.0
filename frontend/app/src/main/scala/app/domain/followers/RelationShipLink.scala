package app.domain.followers

import app.domain.core.AbstractLink
import app.support.JsonModel
import jfx.core.meta.Meta

import scala.scalajs.js

class RelationShipLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[RelationShipLink] with AbstractLink {

  override def meta: Meta[RelationShipLink] = RelationShipLink.meta

  override def name: String = "Followers"

  override def icon: String = "1k_plus"
}

object RelationShipLink {
  val meta : Meta[RelationShipLink] = Meta()}
