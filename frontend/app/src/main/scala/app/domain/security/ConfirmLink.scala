package app.domain.security

import app.domain.core.AbstractLink
import jfx.core.meta.Meta

import scala.scalajs.js

class ConfirmLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends AbstractLink {

  override def name: String = "Bestaetigen"

  override def icon: String = "approval"
}

