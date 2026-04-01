package app.domain.followers

import app.support.JsonModel
import jfx.core.meta.Meta

import scala.scalajs.js

class GroupAssignmentRequest(
  var groupIds: js.Array[String] = js.Array()
) extends JsonModel[GroupAssignmentRequest] {

  override def meta: Meta[GroupAssignmentRequest] = GroupAssignmentRequest.meta
}

object GroupAssignmentRequest {
  val meta : Meta[GroupAssignmentRequest] = Meta(() => new GroupAssignmentRequest())}
