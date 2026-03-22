package app.domain.timeline

import app.domain.core.AbstractLink
import app.support.JsonModel
import jfx.core.macros.property
import jfx.core.state.PropertyAccess

import scala.scalajs.js

class PostsLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[PostsLink] with AbstractLink {

  override def properties: js.Array[PropertyAccess[PostsLink, ?]] =
    PostsLink.properties

  override def name: String = "Posts"

  override def icon: String = "timeline"
}

object PostsLink {
  val properties: js.Array[PropertyAccess[PostsLink, ?]] = js.Array(
    property(_.rel),
    property(_.url),
    property(_.method)
  )
}
