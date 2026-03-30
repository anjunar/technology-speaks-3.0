package app.support

import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}

import scala.scalajs.js

class JsonResponse(
  var status: String = "",
  var message: String | Null = null
) extends JsonModel[JsonResponse] {

  override def properties: Seq[PropertyAccess[JsonResponse, ?]] = JsonResponse.properties
}

object JsonResponse {
  val properties: Seq[PropertyAccess[JsonResponse, ?]] = PropertyMacros.describeProperties[JsonResponse]}
