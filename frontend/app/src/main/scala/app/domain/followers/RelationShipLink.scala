package app.domain.followers

import app.domain.core.AbstractLink
import app.support.JsonModel
import jfx.core.macros.property
import jfx.core.state.PropertyAccess

import scala.scalajs.js

class RelationShipLink(
  var id: String = "followers-list",
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[RelationShipLink] with AbstractLink {

  override def properties: js.Array[PropertyAccess[RelationShipLink, ?]] =
    RelationShipLink.properties

  override def name: String = "Followers"

  override def icon: String = "1k_plus"
}

object RelationShipLink {
  val properties: js.Array[PropertyAccess[RelationShipLink, ?]] = js.Array(
    property(_.id),
    property(_.rel),
    property(_.url),
    property(_.method)
  )
}
