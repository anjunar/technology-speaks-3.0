package app.domain.timeline

import app.domain.core.{AbstractEntity, Data, Link, Table, User}
import app.domain.shared.{Like, OwnerProvider}
import app.support.Api
import jfx.core.meta.Meta
import com.anjunar.scala.enterprise.macros.validation.NotNull
import jfx.core.state.{ListProperty, Property}

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js

class Post extends AbstractEntity[Post] with OwnerProvider {

  val user: Property[User | Null] = Property(null)

  @NotNull(message = "Inhalt ist erforderlich")
  val editor: Property[js.Any | Null] = Property(null)
  val likes: ListProperty[Like] = ListProperty()

  override def meta: Meta[Post] = Post.meta

  def save(): Future[Data[Post]] =
    Api.post("/service/timeline/posts/post", this)

  def update(): Future[Data[Post]] =
    Api.put("/service/timeline/posts/post", this)

  def delete(): Future[Unit] =
    Api.delete("/service/timeline/posts/post", this)
}

object Post {
  val meta: Meta[Post] = Meta()
  def read(id: String): Future[Data[Post]] =
    Api.get(s"/service/timeline/posts/post/$id")

  def list(index: Int, limit: Int): Future[Table[Data[Post]]] =
    Api.get(s"/service/timeline/posts?index=$index&limit=$limit&sort=created:desc")
}
