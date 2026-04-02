package app.domain.core

import jfx.core.meta.Meta

import scala.scalajs.js

class Link(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET",
  var id: String = ""
) extends AbstractLink {

  override def name: String = rel

  override def icon: String = ""
}