package app.domain.timeline

import app.domain.core.{AbstractEntity, Data, Link, Table, User}
import app.domain.shared.{Like, OwnerProvider}
import app.support.Api
import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{ListProperty, Property, PropertyAccess}

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js

class Post(
            val id: Property[UUID] = Property(null),
            val modified: Property[String] = Property(""),
            val created: Property[String] = Property(""),
            val user: Property[User | Null] = Property(null),
            val editor: Property[js.Any | Null] = Property(null),
            val likes: ListProperty[Like] = ListProperty(),
            val links: ListProperty[Link] = ListProperty()
) extends AbstractEntity[Post] with OwnerProvider {

  override def properties: js.Array[PropertyAccess[Post, ?]] =
    Post.properties

  def save(): Future[Data[Post]] =
    Api.post("/service/timeline/posts/post", this)

  def update(): Future[Data[Post]] =
    Api.put("/service/timeline/posts/post", this)

  def delete(): Future[Unit] =
    Api.delete("/service/timeline/posts/post", this)
}

object Post {
  val properties: js.Array[PropertyAccess[Post, ?]] = js.Array(
    typedProperty[Post, Property[UUID], UUID](_.id),
    typedProperty[Post, Property[String], String](_.modified),
    typedProperty[Post, Property[String], String](_.created),
    typedProperty[Post, Property[User | Null], User | Null](_.user),
    typedProperty[Post, Property[js.Any | Null], js.Any | Null](_.editor),
    typedProperty[Post, ListProperty[Like], Like](_.likes),
    typedProperty[Post, ListProperty[Link], Link](_.links)
  )

  def read(id: String): Future[Data[Post]] =
    Api.get(s"/service/timeline/posts/post/$id")

  def list(index: Int, limit: Int): Future[Table[Data[Post]]] =
    Api.get(s"/service/timeline/posts?index=$index&limit=$limit&sort=created:desc")
}
