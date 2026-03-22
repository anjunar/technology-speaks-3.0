package app.domain.shared

import app.domain.core.{AbstractEntity, Data, Link, Table, User}
import app.domain.documents.Issue
import app.domain.timeline.Post
import app.support.Api
import jfx.core.macros.property
import jfx.core.state.{ListProperty, Property, PropertyAccess}

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js

class FirstComment(
                    var id: Property[UUID] = Property(null),
                    var modified: Property[String] = Property(""),
                    var created: Property[String] = Property(""),
                    var user: Property[User | Null] = Property(null),
                    var editor: Property[js.Any | Null] = Property(null),
                    var likes: ListProperty[Like] = ListProperty(),
                    var comments: ListProperty[SecondComment] = ListProperty(),
                    var links: ListProperty[Link] = ListProperty()
) extends AbstractEntity[FirstComment] with OwnerProvider {

  val editable: Property[Boolean] = Property(false)

  override def properties: js.Array[PropertyAccess[FirstComment, ?]] =
    FirstComment.properties

  def save(entity: AbstractEntity[?]): Future[Data[FirstComment]] =
    entity match {
      case issue: Issue =>
        Api.post(s"/service/document/documents/document/issues/issue/${issue.id.get}/comment", this)
      case post: Post =>
        Api.post(s"/service//timeline/posts/post/${post.id.get}/comment", this)
      case _ =>
        Future.failed(RuntimeException("Unknown document type"))
    }

  def update(entity: AbstractEntity[?]): Future[Data[FirstComment]] =
    entity match {
      case issue: Issue =>
        Api.put(s"/service/document/documents/document/issues/issue/${issue.id.get}/comment", this)
      case post: Post =>
        Api.put(s"/service//timeline/posts/post/${post.id.get}/comment", this)
      case _ =>
        Future.failed(RuntimeException("Unknown document type"))
    }

  def delete(entity: AbstractEntity[?]): Future[Unit] =
    entity match {
      case issue: Issue =>
        Api.delete(s"/service/document/documents/document/issues/issue/${issue.id.get}/comment", this)
      case post: Post =>
        Api.delete(s"/service//timeline/posts/post/${post.id.get}/comment", this)
      case _ =>
        Future.failed(RuntimeException("Unknown document type"))
    }
}

object FirstComment {
  val properties: js.Array[PropertyAccess[FirstComment, ?]] = js.Array(
    property(_.id),
    property(_.modified),
    property(_.created),
    property(_.user),
    property(_.editor),
    property(_.likes),
    property(_.comments),
    property(_.links)
  )

  def list(index: Int, limit: Int, post: Post): Future[Table[Data[FirstComment]]] =
    Api.get(s"/service/timeline/posts/post/${post.id.get}/comments?index=$index&limit=$limit&sort=created:desc")

  def list(index: Int, limit: Int, issue: Issue): Future[Table[Data[FirstComment]]] =
    Api.get(s"/service/document/documents/document/issues/issue/${issue.id.get}/comments?index=$index&limit=$limit&sort=created:desc")
}
