package app.domain.timeline

import app.domain.core.AbstractLink
import app.support.JsonModel
import jfx.core.meta.Meta
import jfx.json.JsonType

import scala.scalajs.js

@JsonType("posts-list")
class PostsLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[PostsLink] with AbstractLink {

  override def name: String = "Posts"

  override def icon: String = "timeline"
}


  
