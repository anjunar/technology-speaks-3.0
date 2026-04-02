package app.domain.security

import app.domain.core.UserInfo
import jfx.core.meta.Meta
import jfx.core.state.Property
import jfx.form.validators.{EmailValidator, NotBlankValidator, SizeValidator}

import scala.scalajs.js

class WebAuthnRegister(
  val email: Property[String] = Property(""),
  val nickName: Property[String] = Property("")
)
