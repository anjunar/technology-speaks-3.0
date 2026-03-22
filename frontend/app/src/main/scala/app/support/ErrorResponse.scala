package app.support

import scala.scalajs.js

class ErrorResponse(
  val message: String,
  val path: js.Array[js.Any]
)
