package app.domain.followers

import app.support.JsonModel
import jfx.core.macros.property
import jfx.core.state.PropertyAccess

import scala.scalajs.js

class GroupAssignmentRequest(
  var groupIds: js.Array[String] = js.Array()
) extends JsonModel[GroupAssignmentRequest] {

  override def properties: js.Array[PropertyAccess[GroupAssignmentRequest, ?]] =
    GroupAssignmentRequest.properties
}

object GroupAssignmentRequest {
  val properties: js.Array[PropertyAccess[GroupAssignmentRequest, ?]] = js.Array(
    property(_.groupIds)
  )
}
