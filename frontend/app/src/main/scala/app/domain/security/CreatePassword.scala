package app.domain.security

import jfx.core.meta.Meta
import jfx.core.state.Property

import scala.scalajs.js

class CreatePassword(
  val newPassword: Property[String] = Property(""),
  val confirmPassword: Property[String] = Property("")
)

