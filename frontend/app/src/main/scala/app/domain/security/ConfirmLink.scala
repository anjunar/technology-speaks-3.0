package app.domain.security

import app.domain.core.AbstractLink
import app.support.JsonModel
import jfx.core.meta.Meta

import scala.scalajs.js

class ConfirmLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[ConfirmLink] with AbstractLink {



  override def name: String = "Bestaetigen"

  override def icon: String = "approval"
}

