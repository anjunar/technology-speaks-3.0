package app.domain.security

import app.domain.core.AbstractLink
import app.support.JsonModel
import jfx.core.meta.Meta

import scala.scalajs.js

class PasswordLoginLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[PasswordLoginLink] with AbstractLink {

  override def meta: Meta[PasswordLoginLink] = PasswordLoginLink.meta

  override def name: String = "Login mit Passwort"

  override def icon: String = "login"
}

object PasswordLoginLink {
  val meta : Meta[PasswordLoginLink] = Meta()}
