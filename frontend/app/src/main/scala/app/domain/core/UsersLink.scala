package app.domain.core

import app.support.JsonModel
import jfx.core.meta.Meta

import scala.scalajs.js

class UsersLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[UsersLink] with AbstractLink {

  override def meta: Meta[UsersLink] = UsersLink.meta

  override def name: String = "Benutzer"

  override def icon: String = "diversity_3"
}

object UsersLink {
  val meta : Meta[UsersLink] = Meta()}
