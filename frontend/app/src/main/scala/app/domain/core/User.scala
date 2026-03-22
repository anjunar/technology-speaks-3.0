package app.domain.core

import app.support.Api
import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{ListProperty, Property, PropertyAccess}
import jfx.domain.Media

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js

class User(
            val id: Property[UUID] = Property(null),
            val modified: Property[String] = Property(""),
            val created: Property[String] = Property(""),
            val nickName: Property[String] = Property(""),
            val image: Property[Media | Null] = Property(null),
            val info: Property[UserInfo | Null] = Property(null),
            val address: Property[Address | Null] = Property(null),
            val emails: ListProperty[Email] = ListProperty(),
            val links: ListProperty[Link] = ListProperty()
) extends AbstractEntity[User] {

  override def properties: js.Array[PropertyAccess[User, ?]] =
    User.properties

  def save(): Future[Data[User]] =
    Api.post("/service/core/users/user", this)

  def update(): Future[Data[User]] =
    Api.put("/service/core/users/user", this)

  def delete(): Future[Unit] =
    Api.delete("/service/core/users/user", this)
}

object User {
  val properties: js.Array[PropertyAccess[User, ?]] = js.Array(
    typedProperty[User, Property[UUID], UUID](_.id),
    typedProperty[User, Property[String], String](_.modified),
    typedProperty[User, Property[String], String](_.created),
    typedProperty[User, Property[String], String](_.nickName),
    typedProperty[User, Property[Media | Null], Media | Null](_.image),
    typedProperty[User, Property[UserInfo | Null], UserInfo | Null](_.info),
    typedProperty[User, Property[Address | Null], Address | Null](_.address),
    typedProperty[User, ListProperty[Email], Email](_.emails),
    typedProperty[User, ListProperty[Link], Link](_.links)
  )

  def read(id: String): Future[Data[User]] =
    Api.get(s"/service/core/users/user/$id")

  def list(index: Int, limit: Int): Future[Table[Data[User]]] =
    Api.get(s"/service/core/users?index=$index&limit=$limit&sort=created:desc")
}
