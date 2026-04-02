package app.domain.followers

import app.domain.core.AbstractLink
import jfx.core.meta.Meta
import jfx.json.JsonType

import scala.scalajs.js

@JsonType("followers-list")
class RelationShipLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends AbstractLink {

  override def name: String = "Followers"

  override def icon: String = "1k_plus"
}