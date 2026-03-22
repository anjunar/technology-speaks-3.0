package app.domain.documents

import app.domain.core.{AbstractEntity, Data, Link, Table, User}
import app.domain.shared.OwnerProvider
import app.support.Api
import jfx.core.macros.property
import jfx.core.state.{ListProperty, Property, PropertyAccess}

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js

class Issue(
             var id: Property[UUID] = Property(null),
             var modified: Property[String] = Property(""),
             var created: Property[String] = Property(""),
             var title: Property[String] = Property(""),
             var user: Property[User | Null] = Property(null),
             var editor: Property[js.Any | Null] = Property(null),
             var links: ListProperty[Link] = ListProperty()
) extends AbstractEntity[Issue] with OwnerProvider {

  val editable: Property[Boolean] = Property(false)

  override def properties: js.Array[PropertyAccess[Issue, ?]] =
    Issue.properties
}

object Issue {
  val properties: js.Array[PropertyAccess[Issue, ?]] = js.Array(
    property(_.id),
    property(_.modified),
    property(_.created),
    property(_.title),
    property(_.user),
    property(_.editor),
    property(_.links)
  )

  def read(id: String): Future[Data[Issue]] =
    Api.get(s"/service/document/documents/document/$id/issues/issue")

  def read(documentId: String, issueId: String): Future[Data[Issue]] =
    Api.get(s"/service/document/documents/document/$documentId/issues/issue/$issueId")

  def list(index: Int, limit: Int, document: Document): Future[Table[Data[Issue]]] =
    Api.get(s"/service/document/documents/document/${document.id.get}/issues?index=$index&limit=$limit&sort=created:desc")
}
