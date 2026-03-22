package app.domain.shared

import app.domain.core.{AbstractEntity, Data, Link, User}
import app.domain.documents.Issue
import app.domain.timeline.Post
import app.support.Api
import jfx.core.macros.property
import jfx.core.state.{ListProperty, Property, PropertyAccess}

import scala.concurrent.Future
import scala.scalajs.js

class SecondComment(
  var id: Property[String] = Property(""),
  var modified: Property[String] = Property(""),
  var created: Property[String] = Property(""),
  var user: Property[User | Null] = Property(null),
  var editor: Property[js.Any | Null] = Property(null),
  var likes: ListProperty[Like] = ListProperty(),
  var links: ListProperty[Link] = ListProperty()
) extends AbstractEntity[SecondComment] with OwnerProvider {

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
    property(_.id),
    property(_.modified),
    property(_.created),
    property(_.user),
    property(_.editor),
    property(_.likes),
    property(_.links)
  )
}
