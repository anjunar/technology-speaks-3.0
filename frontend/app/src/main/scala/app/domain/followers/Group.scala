package app.domain.followers

import app.domain.core.{AbstractEntity, Data, Link, Table, User}
import app.support.Api
import jfx.core.macros.property
import jfx.core.state.{ListProperty, Property, PropertyAccess}

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js

class Group(
             var id: Property[UUID] = Property(null),
             var name: Property[String] = Property(""),
             var modified: Property[String] = Property(""),
             var created: Property[String] = Property(""),
             var users: ListProperty[User] = ListProperty(),
             var links: ListProperty[Link] = ListProperty()
) extends AbstractEntity[Group] {

  val editable: Property[Boolean] = Property(false)

  override def properties: js.Array[PropertyAccess[Group, ?]] =
    Group.properties

  def save(): Future[Data[Group]] =
    Api.post("/service/followers/groups/groups", this)

  def update(): Future[Data[Group]] =
    Api.put("/service/followers/groups/groups", this)

  def delete(): Future[Unit] =
    Api.delete("/service/followers/groups/groups", this)
}

object Group {
  val properties: js.Array[PropertyAccess[Group, ?]] = js.Array(
    property(_.id),
    property(_.name),
    property(_.modified),
    property(_.created),
    property(_.users),
    property(_.links)
  )

  def read(id: String): Future[Data[Group]] =
    Api.get(s"/service/followers/groups/groups/$id")

  def list(index: Int, limit: Int): Future[Table[Data[Group]]] =
    Api.get(s"/service/followers/groups?index=$index&limit=$limit&sort=created:desc")
}
