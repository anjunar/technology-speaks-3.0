package app.domain.timeline

import app.domain.core.{AbstractEntity, Data, Link, Table, User}
import app.domain.shared.{Like, OwnerProvider}
import app.support.Api
import jfx.core.macros.property
import jfx.core.state.{ListProperty, Property, PropertyAccess}

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js

class Post(
            var id: Property[UUID] = Property(null),
            var modified: Property[String] = Property(""),
            var created: Property[String] = Property(""),
            var user: Property[User | Null] = Property(null),
            var editor: Property[js.Any | Null] = Property(null),
            var likes: ListProperty[Like] = ListProperty(),
            var links: ListProperty[Link] = ListProperty()
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
    property(_.id),
    property(_.modified),
    property(_.created),
    property(_.user),
    property(_.editor),
    property(_.likes),
    property(_.links)
  )

  def read(id: String): Future[Data[Post]] =
    Api.get(s"/service/timeline/posts/post/$id")

  def list(index: Int, limit: Int): Future[Table[Data[Post]]] =
    Api.get(s"/service/timeline/posts?index=$index&limit=$limit&sort=created:desc")
}
