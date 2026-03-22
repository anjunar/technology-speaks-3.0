package app.support

import jfx.core.macros.property
import jfx.core.state.PropertyAccess

import scala.scalajs.js

class JsonResponse(
  var status: String = "",
  var message: String | Null = null
) extends JsonModel[JsonResponse] {

  override def properties: js.Array[PropertyAccess[JsonResponse, ?]] =
    JsonResponse.properties
}

object JsonResponse {
  val properties: js.Array[PropertyAccess[JsonResponse, ?]] = js.Array(
    property(_.status),
    property(_.message)
  )
}
