package app.domain.documents

import app.domain.core.{AbstractEntity, Data, Link, Table, User}
import app.domain.shared.OwnerProvider
import app.support.Api
import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{ListProperty, Property, PropertyAccess}

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js

class Issue(
             val id: Property[UUID] = Property(null),
             val modified: Property[String] = Property(""),
             val created: Property[String] = Property(""),
             val title: Property[String] = Property(""),
             val user: Property[User | Null] = Property(null),
             val editor: Property[js.Any | Null] = Property(null),
             val links: ListProperty[Link] = ListProperty()
) extends AbstractEntity[Issue] with OwnerProvider {

  val editable: Property[Boolean] = Property(false)

  override def properties: js.Array[PropertyAccess[Issue, ?]] =
    Issue.properties
}

object Issue {
  val properties: js.Array[PropertyAccess[Issue, ?]] = js.Array(
    typedProperty[Issue, Property[UUID], UUID](_.id),
    typedProperty[Issue, Property[String], String](_.modified),
    typedProperty[Issue, Property[String], String](_.created),
    typedProperty[Issue, Property[String], String](_.title),
    typedProperty[Issue, Property[User | Null], User | Null](_.user),
    typedProperty[Issue, Property[js.Any | Null], js.Any | Null](_.editor),
    typedProperty[Issue, ListProperty[Link], Link](_.links)
  )

  def read(id: String): Future[Data[Issue]] =
    Api.get(s"/service/document/documents/document/$id/issues/issue")

  def read(documentId: String, issueId: String): Future[Data[Issue]] =
    Api.get(s"/service/document/documents/document/$documentId/issues/issue/$issueId")

  def list(index: Int, limit: Int, document: Document): Future[Table[Data[Issue]]] =
    Api.get(s"/service/document/documents/document/${document.id.get}/issues?index=$index&limit=$limit&sort=created:desc")
}
