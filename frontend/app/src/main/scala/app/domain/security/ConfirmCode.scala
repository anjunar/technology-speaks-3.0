package app.domain.security

import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import jfx.core.state.Property

import scala.scalajs.js

class ConfirmCode(
  val confirm: Property[String] = Property("")
) extends JsonModel[ConfirmCode] {

  override def properties: Seq[PropertyAccess[ConfirmCode, ?]] = ConfirmCode.properties
}

object ConfirmCode {
  val properties: Seq[PropertyAccess[ConfirmCode, ?]]= PropertyMacros.describeProperties[ConfirmCode]}
