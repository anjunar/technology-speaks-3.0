package app.domain.followers

import app.domain.core.{AbstractEntity, Data, Link, Table, User}
import app.support.Api
import jfx.core.macros.property
import jfx.core.state.{ListProperty, Property, PropertyAccess}

import scala.concurrent.Future
import scala.scalajs.js

class RelationShip(
  var id: Property[String] = Property(""),
  var modified: Property[String] = Property(""),
  var created: Property[String] = Property(""),
  var follower: Property[User | Null] = Property(null),
  var users: ListProperty[User] = ListProperty(),
  var groups: ListProperty[Group] = ListProperty(),
  var links: ListProperty[Link] = ListProperty()
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
    property(_.id),
    property(_.modified),
    property(_.created),
    property(_.follower),
    property(_.users),
    property(_.groups),
    property(_.links)
  )

  def read(id: String): Future[Data[RelationShip]] =
    Api.get(s"/service/followers/relationships/relationship/$id")

  def list(index: Int, limit: Int): Future[Table[Data[RelationShip]]] =
    Api.get(s"/service/followers/relationships?index=$index&limit=$limit&sort=created:desc")
}
