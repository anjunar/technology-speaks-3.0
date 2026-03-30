package app.domain.followers

import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}

import scala.scalajs.js

class GroupAssignmentRequest(
  var groupIds: js.Array[String] = js.Array()
) extends JsonModel[GroupAssignmentRequest] {

  override def properties: Seq[PropertyAccess[GroupAssignmentRequest, ?]] = GroupAssignmentRequest.properties
}

object GroupAssignmentRequest {
  val properties: Seq[PropertyAccess[GroupAssignmentRequest, ?]]= PropertyMacros.describeProperties[GroupAssignmentRequest]}
