package app.domain.followers

import app.domain.core.{AbstractEntity, Data, Link, Table, User}
import app.support.Api
import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{ListProperty, Property, PropertyAccess}

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js

class Group(
             val id: Property[UUID] = Property(null),
             val name: Property[String] = Property(""),
             val modified: Property[String] = Property(""),
             val created: Property[String] = Property(""),
             val users: ListProperty[User] = ListProperty(),
             val links: ListProperty[Link] = ListProperty()
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
    typedProperty[Group, Property[UUID], UUID](_.id),
    typedProperty[Group, Property[String], String](_.name),
    typedProperty[Group, Property[String], String](_.modified),
    typedProperty[Group, Property[String], String](_.created),
    typedProperty[Group, ListProperty[User], User](_.users),
    typedProperty[Group, ListProperty[Link], Link](_.links)
  )

  def read(id: String): Future[Data[Group]] =
    Api.get(s"/service/followers/groups/groups/$id")

  def list(index: Int, limit: Int): Future[Table[Data[Group]]] =
    Api.get(s"/service/followers/groups?index=$index&limit=$limit&sort=created:desc")
}
