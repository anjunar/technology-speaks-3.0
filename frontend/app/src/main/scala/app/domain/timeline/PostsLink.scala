package app.domain.timeline

import app.domain.core.AbstractLink
import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}

import scala.scalajs.js

class PostsLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[PostsLink] with AbstractLink {

  override def properties: Seq[PropertyAccess[PostsLink, ?]] = PostsLink.properties

  override def name: String = "Posts"

  override def icon: String = "timeline"
}

object PostsLink {
  val properties: Seq[PropertyAccess[PostsLink, ?]]= PropertyMacros.describeProperties[PostsLink]}
