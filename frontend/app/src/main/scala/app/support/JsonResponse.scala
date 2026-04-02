package app.support

import scala.scalajs.js

class JsonResponse(
  var status: String = "",
  var message: String | Null = null
) extends JsonModel[JsonResponse]

object JsonResponse {
}
