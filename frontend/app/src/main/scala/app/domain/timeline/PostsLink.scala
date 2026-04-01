package app.domain.timeline

import app.domain.core.AbstractLink
import app.support.JsonModel
import jfx.core.meta.Meta

import scala.scalajs.js

class PostsLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[PostsLink] with AbstractLink {

  override def meta: Meta[PostsLink] = PostsLink.meta

  override def name: String = "Posts"

  override def icon: String = "timeline"
}

object PostsLink {
  val meta : Meta[PostsLink] = Meta(() => new PostsLink())}
