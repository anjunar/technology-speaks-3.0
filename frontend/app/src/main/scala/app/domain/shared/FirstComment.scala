package app.domain.shared

import app.domain.core.{AbstractEntity, Data, Link, Table, User}
import app.domain.documents.Issue
import app.domain.timeline.Post
import app.support.Api
import app.support.Api.given
import jfx.core.state.{ListProperty, Property}
import jfx.core.meta.Meta

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js

class FirstComment extends AbstractEntity with OwnerProvider {

  val user: Property[User | Null] = Property(null)
  val editor: Property[js.Any | Null] = Property(null)
  val likes: ListProperty[Like] = ListProperty()
  val comments: ListProperty[SecondComment] = ListProperty()

  val editable: Property[Boolean] = Property(false)



  def save(entity: AbstractEntity): Future[Data[FirstComment]] =
    entity match {
      case issue: Issue =>
        Api.request(s"/service/document/documents/document/issues/issue/${issue.id.get}/comment").post(this).read[Data[FirstComment]]
      case post: Post =>
        Api.request(s"/service/timeline/posts/post/${post.id.get}/comment").post(this).read[Data[FirstComment]]
      case _ =>
        Future.failed(RuntimeException("Unknown document type"))
    }

  def update(entity: AbstractEntity): Future[Data[FirstComment]] =
    entity match {
      case issue: Issue =>
        Api.request(s"/service/document/documents/document/issues/issue/${issue.id.get}/comment").put(this).read[Data[FirstComment]]
      case post: Post =>
        Api.request(s"/service/timeline/posts/post/${post.id.get}/comment").put(this).read[Data[FirstComment]]
      case _ =>
        Future.failed(RuntimeException("Unknown document type"))
    }

  def delete(entity: AbstractEntity): Future[Unit] =
    entity match {
      case issue: Issue =>
        Api.request(s"/service/document/documents/document/issues/issue/${issue.id.get}/comment").delete(this).unit
      case post: Post =>
        Api.request(s"/service/timeline/posts/post/${post.id.get}/comment").delete(this).unit
      case _ =>
        Future.failed(RuntimeException("Unknown document type"))
    }
}

object FirstComment {


  def list(index: Int, limit: Int, post: Post): Future[Table[Data[FirstComment]]] =
    Api.request(s"/service/timeline/posts/post/${post.id.get}/comments?index=$index&limit=$limit&sort=created:desc").get.read[Table[Data[FirstComment]]]

  def list(index: Int, limit: Int, issue: Issue): Future[Table[Data[FirstComment]]] =
    Api.request(s"/service/document/documents/document/issues/issue/${issue.id.get}/comments?index=$index&limit=$limit&sort=created:desc").get.read[Table[Data[FirstComment]]]
}
