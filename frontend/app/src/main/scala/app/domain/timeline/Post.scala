package app.domain.timeline

import app.domain.core.{AbstractEntity, Data, Link, Table, User}
import app.domain.shared.{Like, OwnerProvider}
import app.support.Api
import app.support.Api.given
import jfx.core.meta.Meta
import jfx.core.state.{ListProperty, Property}
import jfx.form.validators.*

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js

class Post extends AbstractEntity with OwnerProvider {

  val user: Property[User | Null] = Property(null)

  @NotNull(message = "Inhalt ist erforderlich")
  val editor: Property[js.Any | Null] = Property(null)
  val likes: ListProperty[Like] = ListProperty()



  def save(): Future[Data[Post]] =
    Api.post("/service/timeline/posts/post", this).map(raw => Api.deserialize[Data[Post]](raw))

  def update(): Future[Data[Post]] =
    Api.put("/service/timeline/posts/post", this).map(raw => Api.deserialize[Data[Post]](raw))

  def delete(): Future[Unit] =
    Api.delete("/service/timeline/posts/post", this)
}

object Post {

  def read(id: String): Future[Data[Post]] =
    Api.get(s"/service/timeline/posts/post/$id").map(raw => Api.deserialize[Data[Post]](raw))

  def list(index: Int, limit: Int): Future[Table[Data[Post]]] =
    Api.get(s"/service/timeline/posts?index=$index&limit=$limit&sort=created:desc").map(raw => Api.deserialize[Table[Data[Post]]](raw))
}
