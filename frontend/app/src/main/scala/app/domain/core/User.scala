package app.domain.core

import app.support.Api
import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{ListProperty, Property, PropertyAccess}
import jfx.domain.Media

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js

class User(
            var id: Property[UUID] = Property(null),
            var modified: Property[String] = Property(""),
            var created: Property[String] = Property(""),
            var nickName: Property[String] = Property(""),
            var image: Property[Media | Null] = Property(null),
            var info: Property[UserInfo | Null] = Property(null),
            var address: Property[Address | Null] = Property(null),
            var emails: ListProperty[Email] = ListProperty(),
            var links: ListProperty[Link] = ListProperty()
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
    property(_.modified),
    property(_.created),
    property(_.nickName),
    property(_.image),
    property(_.info),
    property(_.address),
    property(_.emails),
    property(_.links)
  )

  def read(id: String): Future[Data[User]] =
    Api.get(s"/service/core/users/user/$id")

  def list(index: Int, limit: Int): Future[Table[Data[User]]] =
    Api.get(s"/service/core/users?index=$index&limit=$limit&sort=created:desc")
}
