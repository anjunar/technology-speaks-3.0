package app.domain.security

import app.support.JsonModel
import jfx.core.meta.Meta
import jfx.core.state.Property

import scala.scalajs.js

class ConfirmCode(
  val confirm: Property[String] = Property("")
) extends JsonModel[ConfirmCode] {

  override def meta: Meta[ConfirmCode] = ConfirmCode.meta
}

object ConfirmCode {
  val meta : Meta[ConfirmCode] = Meta()}
