package app.domain.shared

import app.domain.core.{AbstractEntity, Data, Link, User}
import app.domain.documents.Issue
import app.domain.timeline.Post
import app.support.Api
import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{ListProperty, Property, PropertyAccess}

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js

class SecondComment extends AbstractEntity[SecondComment] with OwnerProvider {

  val user: Property[User | Null] = Property(null)
  val editor: Property[js.Any | Null] = Property(null)
  val likes: ListProperty[Like] = ListProperty()

  val editable: Property[Boolean] = Property(false)

  override def properties: js.Array[PropertyAccess[SecondComment, ?]] =
    SecondComment.properties

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
  val properties: js.Array[PropertyAccess[SecondComment, ?]] = js.Array(
    typedProperty[SecondComment, Property[UUID], UUID](_.id),
    typedProperty[SecondComment, Property[String], String](_.modified),
    typedProperty[SecondComment, Property[String], String](_.created),
    typedProperty[SecondComment, Property[User | Null], User | Null](_.user),
    typedProperty[SecondComment, Property[js.Any | Null], js.Any | Null](_.editor),
    typedProperty[SecondComment, ListProperty[Like], Like](_.likes),
    typedProperty[SecondComment, ListProperty[Link], Link](_.links)
  )
}
