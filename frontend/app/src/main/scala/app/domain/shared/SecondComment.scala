package app.domain.shared

import app.domain.core.{AbstractEntity, Data, Link, User}
import app.domain.documents.Issue
import app.domain.timeline.Post
import app.support.Api
import jfx.core.meta.Meta
import jfx.core.state.{ListProperty, Property}

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js

class SecondComment extends AbstractEntity[SecondComment] with OwnerProvider {

  val user: Property[User | Null] = Property(null)
  val editor: Property[js.Any | Null] = Property(null)
  val likes: ListProperty[Like] = ListProperty()

  val editable: Property[Boolean] = Property(false)

  override def meta: Meta[SecondComment] = SecondComment.meta

  def save(issue: Issue): Future[Data[FirstComment]] =
    Api.post(s"/service/document/documents/document/issues/issue/${issue.id.get}/comment", this)

  def save(post: Post): Future[Data[FirstComment]] =
    Api.post(s"/service//timeline/posts/post/${post.id.get}/comment", this)

  def update(issue: Issue): Future[Data[FirstComment]] =
    Api.put(s"/service/document/documents/document/issues/issue/${issue.id.get}/comment", this)

  def update(post: Post): Future[Data[FirstComment]] =
    Api.put(s"/service//timeline/posts/post/${post.id.get}/comment", this)
}

object SecondComment {
  val meta: Meta[SecondComment] = Meta(() => new SecondComment())}
