package app.domain.documents

import app.domain.core.{AbstractEntity, Data, Link, Table, User}
import app.domain.shared.OwnerProvider
import app.support.Api
import jfx.core.macros.property
import jfx.core.state.{ListProperty, Property, PropertyAccess}

import scala.concurrent.Future
import scala.scalajs.js

class Document(
  var id: Property[String] = Property(""),
  var modified: Property[String] = Property(""),
  var created: Property[String] = Property(""),
  var title: Property[String] = Property(""),
  var user: Property[User | Null] = Property(null),
  var editor: Property[js.Any | Null] = Property(null),
  var links: ListProperty[Link] = ListProperty()
) extends AbstractEntity[Document] with OwnerProvider {

  val editable: Property[Boolean] = Property(false)

  override def properties: js.Array[PropertyAccess[Document, ?]] =
    Document.properties

  def save(): Future[Data[Document]] =
    Api.post("/service/document/documents/document", this)

  def update(): Future[Data[Document]] =
    Api.put("/service/document/documents/document", this)

  def delete(): Future[Unit] =
    Api.delete("/service/document/documents/document", this)
}

object Document {
  val properties: js.Array[PropertyAccess[Document, ?]] = js.Array(
    property(_.id),
    property(_.modified),
    property(_.created),
    property(_.title),
    property(_.user),
    property(_.editor),
    property(_.links)
  )

  def root(): Future[Data[Document]] =
    Api.post("/service/document/documents/document/root")

  def list(index: Int, limit: Int): Future[Table[Data[Document]]] =
    Api.get(s"/service/document/documents?index=$index&limit=$limit&sort=created:desc")
}
