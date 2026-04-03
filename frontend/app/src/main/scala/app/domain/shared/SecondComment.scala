package app.domain.shared

import app.domain.core.{AbstractEntity, Data, Link, User}
import app.domain.documents.Issue
import app.domain.timeline.Post
import app.support.Api
import app.support.Api.given
import jfx.core.meta.Meta
import jfx.core.state.{ListProperty, Property}

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js

class SecondComment extends AbstractEntity with OwnerProvider {

  val user: Property[User | Null] = Property(null)
  val editor: Property[js.Any | Null] = Property(null)
  val likes: ListProperty[Like] = ListProperty()

  val editable: Property[Boolean] = Property(false)



  def save(issue: Issue): Future[Data[FirstComment]] =
    Api.request(s"/service/document/documents/document/issues/issue/${issue.id.get}/comment").post(this).read[Data[FirstComment]]

  def save(post: Post): Future[Data[FirstComment]] =
    Api.request(s"/service/timeline/posts/post/${post.id.get}/comment").post(this).read[Data[FirstComment]]

  def update(issue: Issue): Future[Data[FirstComment]] =
    Api.request(s"/service/document/documents/document/issues/issue/${issue.id.get}/comment").put(this).read[Data[FirstComment]]

  def update(post: Post): Future[Data[FirstComment]] =
    Api.request(s"/service/timeline/posts/post/${post.id.get}/comment").put(this).read[Data[FirstComment]]
}

object SecondComment {

}
