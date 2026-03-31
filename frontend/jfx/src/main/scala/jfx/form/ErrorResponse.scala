package jfx.form

import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}

import scala.scalajs.js

class ErrorResponse(
  var message: String = "",
  var path: js.Array[Any] = new js.Array[Any]()
) extends Model[ErrorResponse] {
  override def properties: Seq[PropertyAccess[ErrorResponse, ?]] = ErrorResponse.properties
}

object ErrorResponse {
  val properties: Seq[PropertyAccess[ErrorResponse, ?]] = PropertyMacros.describeProperties[ErrorResponse]
}
