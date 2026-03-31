package app.domain.security

import app.domain.core.UserInfo
import app.support.JsonModel
import jfx.core.meta.Meta
import jfx.core.state.Property

import scala.scalajs.js

class WebAuthnLogin(
  val email: Property[String] = Property("")
) extends JsonModel[WebAuthnLogin] {

  override def meta: Meta[WebAuthnLogin] = WebAuthnLogin.meta
}

object WebAuthnLogin {
  val meta: Meta[WebAuthnLogin] = Meta()
}
