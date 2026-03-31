package app.domain.core

import app.support.JsonModel
import jfx.core.meta.Meta

import scala.scalajs.js

case class Link(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET",
  var id: String = ""
) extends JsonModel[Link] {

  override def meta: Meta[Link] = Link.meta
}

object Link {
  val meta: Meta[Link] = Meta()
}
