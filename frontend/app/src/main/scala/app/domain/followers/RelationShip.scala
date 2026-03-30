package app.domain.followers

import app.domain.core.{AbstractEntity, Data, Link, Table, User}
import app.support.{Api, AppJson}
import app.support.Api.given
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import jfx.core.state.{ListProperty, Property}

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.URIUtils.encodeURIComponent

class RelationShip extends AbstractEntity[RelationShip] {

  val follower: Property[User | Null] = Property(null)
  val users: ListProperty[User] = ListProperty()
  val groups: ListProperty[Group] = ListProperty()

  override def properties: Seq[PropertyAccess[RelationShip, ?]] = RelationShip.properties

  def save(): Future[Data[RelationShip]] =
    Api.post("/service/followers/relationships/relationship", this)

  def update(): Future[Data[RelationShip]] =
    Api.put("/service/followers/relationships/relationship", this)

  def delete(): Future[Unit] =
    Api.delete("/service/followers/relationships/relationship", this)

  def updateGroups(selectedGroups: IterableOnce[Group]): Future[RelationShip] = {
    val followerId = Option(follower.get).flatMap(user => Option(user.id.get)).map(_.toString)
    followerId match {
      case Some(id) =>
        val request = new GroupAssignmentRequest(
          selectedGroups.iterator.flatMap(group => Option(group.id.get).map(_.toString)).toSeq.toJSArray
        )

        Api
          .requestJson("PUT", s"/service/core/users/user/$id/groups", request)
          .map { raw =>
            groups.setAll(RelationShip.deserializeAssignedGroups(raw))
            this
          }
      case None =>
        Future.successful(this)
    }
  }
}

object RelationShip {
  val properties: Seq[PropertyAccess[RelationShip, ?]] = PropertyMacros.describeProperties[RelationShip]
  def read(id: String): Future[Data[RelationShip]] =
    Api.get(s"/service/followers/relationships/relationship/$id")

  def list(
    index: Int,
    limit: Int,
    query: String = "",
    groups: Seq[Group] = Seq.empty,
    sorting: Seq[String] = Seq("created:desc")
  ): Future[Table[Data[RelationShip]]] = {
    val normalizedQuery = Option(query).map(_.trim).getOrElse("")
    val queryParameter =
      if (normalizedQuery.isEmpty) ""
      else s"&name=${encodeURIComponent(normalizedQuery)}"
    val sortParameter = renderSortParameters(sorting)

    val groupParameters =
      groups.iterator
        .flatMap(group => Option(group.id.get).map(_.toString))
        .map(id => s"groups=${encodeURIComponent(id)}")
        .mkString("&")

    val groupsSuffix =
      if (groupParameters.isEmpty) ""
      else s"&$groupParameters"

    Api.get(s"/service/followers/relationships?index=$index&limit=$limit$sortParameter$queryParameter$groupsSuffix")
  }

  private def renderSortParameters(sorting: Seq[String]): String = {
    val normalizedSorting = sorting.iterator.map(_.trim).filter(_.nonEmpty).toVector
    if (normalizedSorting.isEmpty) ""
    else normalizedSorting.map(value => s"&sort=${encodeURIComponent(value)}").mkString
  }

  private def deserializeAssignedGroups(raw: js.Any): Seq[Group] =
    if (raw == null || js.isUndefined(raw) || !js.Array.isArray(raw)) {
      Seq.empty
    } else {
      raw
        .asInstanceOf[js.Array[js.Any]]
        .iterator
        .collect {
          case value if value != null && !js.isUndefined(value) =>
            AppJson.mapper.deserialize(value.asInstanceOf[js.Dynamic]).asInstanceOf[Data[Group]].data
        }
        .toSeq
    }
}
