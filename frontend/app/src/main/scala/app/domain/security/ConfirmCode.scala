package app.domain.security

import app.support.JsonModel
import jfx.core.meta.Meta
import jfx.core.state.Property

import scala.scalajs.js

class ConfirmCode(
  val confirm: Property[String] = Property("")
) extends JsonModel[ConfirmCode] {


}
