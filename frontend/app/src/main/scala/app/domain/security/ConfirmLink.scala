package app.domain.security

import app.domain.core.AbstractLink
import app.support.JsonModel
import jfx.core.macros.property
import jfx.core.state.PropertyAccess

import scala.scalajs.js

class ConfirmLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[ConfirmLink] with AbstractLink {

  override def properties: js.Array[PropertyAccess[ConfirmLink, ?]] =
    ConfirmLink.properties

  override def name: String = "Bestaetigen"

  override def icon: String = "approval"
}

object ConfirmLink {
  val properties: js.Array[PropertyAccess[ConfirmLink, ?]] = js.Array(
    property(_.rel),
    property(_.url),
    property(_.method)
  )
}
