package app.domain.core

import app.support.JsonModel
import jfx.core.macros.property
import jfx.core.state.PropertyAccess

import scala.scalajs.js

case class Link(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET",
  var id: String = ""
) extends JsonModel[Link] {

  override def properties: js.Array[PropertyAccess[Link, ?]] =
    Link.properties
}

object Link {
  val properties: js.Array[PropertyAccess[Link, ?]] = js.Array(
    property(_.rel),
    property(_.url),
    property(_.method),
    property(_.id)
  )
}
