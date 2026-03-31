package jfx.form

import jfx.core.meta.Meta

import scala.scalajs.js

class ErrorResponse(
  var message: String = "",
  var path: js.Array[Any] = new js.Array[Any]()
) extends Model[ErrorResponse] {
  override def meta: Meta[ErrorResponse] = ErrorResponse.meta
}

object ErrorResponse {
  val meta: Meta[ErrorResponse] = Meta(() => new ErrorResponse())
}
