package app.domain.security

import app.support.JsonModel
import jfx.core.macros.property
import jfx.core.state.{Property, PropertyAccess}

import scala.scalajs.js

class ConfirmCode(
  var confirm: Property[String] = Property("")
) extends JsonModel[ConfirmCode] {

  override def properties: js.Array[PropertyAccess[ConfirmCode, ?]] =
    ConfirmCode.properties
}

object ConfirmCode {
  val properties: js.Array[PropertyAccess[ConfirmCode, ?]] = js.Array(
    property(_.confirm)
  )
}
