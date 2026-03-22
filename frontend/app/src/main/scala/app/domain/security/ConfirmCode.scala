package app.domain.security

import app.support.JsonModel
import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{Property, PropertyAccess}

import scala.scalajs.js

class ConfirmCode(
  val confirm: Property[String] = Property("")
) extends JsonModel[ConfirmCode] {

  override def properties: js.Array[PropertyAccess[ConfirmCode, ?]] =
    ConfirmCode.properties
}

object ConfirmCode {
  val properties: js.Array[PropertyAccess[ConfirmCode, ?]] = js.Array(
    typedProperty[ConfirmCode, Property[String], String](_.confirm)
  )
}
