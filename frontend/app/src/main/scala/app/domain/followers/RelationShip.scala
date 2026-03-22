package app.domain.followers

import app.domain.core.{AbstractEntity, Data, Link, Table, User}
import app.support.Api
import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{ListProperty, Property, PropertyAccess}

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js

class RelationShip(
                    val id: Property[UUID] = Property(null),
                    val modified: Property[String] = Property(""),
                    val created: Property[String] = Property(""),
                    val follower: Property[User | Null] = Property(null),
                    val users: ListProperty[User] = ListProperty(),
                    val groups: ListProperty[Group] = ListProperty(),
                    val links: ListProperty[Link] = ListProperty()
) extends AbstractEntity[RelationShip] {

  override def properties: js.Array[PropertyAccess[RelationShip, ?]] =
    RelationShip.properties

  def save(): Future[Data[RelationShip]] =
    Api.post("/service/followers/relationships/relationship", this)

  def update(): Future[Data[RelationShip]] =
    Api.put("/service/followers/relationships/relationship", this)

  def delete(): Future[Unit] =
    Api.delete("/service/followers/relationships/relationship", this)
}

object RelationShip {
  val properties: js.Array[PropertyAccess[RelationShip, ?]] = js.Array(
    typedProperty[RelationShip, Property[UUID], UUID](_.id),
    typedProperty[RelationShip, Property[String], String](_.modified),
    typedProperty[RelationShip, Property[String], String](_.created),
    typedProperty[RelationShip, Property[User | Null], User | Null](_.follower),
    typedProperty[RelationShip, ListProperty[User], User](_.users),
    typedProperty[RelationShip, ListProperty[Group], Group](_.groups),
    typedProperty[RelationShip, ListProperty[Link], Link](_.links)
  )

  def read(id: String): Future[Data[RelationShip]] =
    Api.get(s"/service/followers/relationships/relationship/$id")

  def list(index: Int, limit: Int): Future[Table[Data[RelationShip]]] =
    Api.get(s"/service/followers/relationships?index=$index&limit=$limit&sort=created:desc")
}
