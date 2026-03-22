package app.domain.core

import app.support.JsonModel
import jfx.core.macros.property
import jfx.core.state.PropertyAccess

import scala.scalajs.js

class UsersLink(
  var id: String = "users-list",
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[UsersLink] with AbstractLink {

  override def properties: js.Array[PropertyAccess[UsersLink, ?]] =
    UsersLink.properties

  override def name: String = "Benutzer"

  override def icon: String = "diversity_3"
}

object UsersLink {
  val properties: js.Array[PropertyAccess[UsersLink, ?]] = js.Array(
    property(_.id),
    property(_.rel),
    property(_.url),
    property(_.method)
  )
}
