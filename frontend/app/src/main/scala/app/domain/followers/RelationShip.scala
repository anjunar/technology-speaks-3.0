package app.domain.followers

import app.domain.core.{AbstractEntity, Data, Link, Table, User}
import app.support.Api
import app.support.Api.given
import jfx.core.state.{ListProperty, Property}
import jfx.json.JsonMapper
import reflect.macros.ReflectMacros.reflectType

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.URIUtils.encodeURIComponent

class RelationShip extends AbstractEntity {

  val follower: Property[User | Null] = Property(null)
  val users: ListProperty[User] = ListProperty()
  val groups: ListProperty[Group] = ListProperty()

  def save(): Future[Data[RelationShip]] =
    Api.request("/service/followers/relationships/relationship").post(this).read[Data[RelationShip]]

  def update(): Future[Data[RelationShip]] =
    Api.request("/service/followers/relationships/relationship").put(this).read[Data[RelationShip]]

  def delete(): Future[Unit] =
    Api.request("/service/followers/relationships/relationship").delete(this).unit

  def updateGroups(selectedGroups: IterableOnce[Group]): Future[RelationShip] = {
    val followerId = Option(follower.get).flatMap(user => Option(user.id.get)).map(_.toString)
    followerId match {
      case Some(id) =>
        val request = new GroupAssignmentRequest(
          selectedGroups.iterator.flatMap(group => Option(group.id.get).map(_.toString)).toSeq.toJSArray
        )

        Api
          .request(s"/service/core/users/user/$id/groups")
          .put(request)
          .raw[js.Array[js.Any]]
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

  def read(id: String): Future[Data[RelationShip]] =
    Api.request(s"/service/followers/relationships/relationship/$id").get.read[Data[RelationShip]]

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

    Api.request(s"/service/followers/relationships?index=$index&limit=$limit$sortParameter$queryParameter$groupsSuffix").get.read[Table[Data[RelationShip]]]
  }

  private def renderSortParameters(sorting: Seq[String]): String = {
    val normalizedSorting = sorting.iterator.map(_.trim).filter(_.nonEmpty).toVector
    if (normalizedSorting.isEmpty) ""
    else normalizedSorting.map(value => s"&sort=${encodeURIComponent(value)}").mkString
  }

  private def deserializeAssignedGroups(raw: js.Array[js.Any]): Seq[Group] =
    if (raw == null || js.isUndefined(raw) || !js.Array.isArray(raw)) {
      Seq.empty
    } else {
      raw
        .iterator
        .collect {
          case value if value != null && !js.isUndefined(value) =>
            JsonMapper.deserialize(value.asInstanceOf[js.Dynamic], reflectType[Group]).asInstanceOf[Group]
        }
        .toSeq
    }
}
