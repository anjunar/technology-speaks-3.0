package app.support

import jfx.core.meta.Meta

import scala.scalajs.js

class JsonResponse(
  var status: String = "",
  var message: String | Null = null
) extends JsonModel[JsonResponse] {

  override def meta: Meta[JsonResponse] = JsonResponse.meta
}

object JsonResponse {
  val meta: Meta[JsonResponse] = Meta(() => new JsonResponse())}
