package app.domain.core

import app.support.JsonModel
import jfx.core.meta.Meta
import jfx.json.JsonType

import scala.scalajs.js

@JsonType("users-list")
class UsersLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[UsersLink] with AbstractLink {



  override def name: String = "Benutzer"

  override def icon: String = "diversity_3"
}