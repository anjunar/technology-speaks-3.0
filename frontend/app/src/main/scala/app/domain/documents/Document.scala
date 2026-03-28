package app.domain.documents

import app.domain.core.{AbstractEntity, Data, Link, Table, User}
import app.domain.shared.OwnerProvider
import app.support.Api
import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{ListProperty, Property, PropertyAccess}

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.URIUtils.encodeURIComponent

class Document extends AbstractEntity[Document] with OwnerProvider {

  val title: Property[String] = Property("")
  val user: Property[User | Null] = Property(null)
  val editor: Property[js.Any | Null] = Property(null)

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
    typedProperty[Document, Property[UUID], UUID](_.id),
    typedProperty[Document, Property[String], String](_.modified),
    typedProperty[Document, Property[String], String](_.created),
    typedProperty[Document, Property[String], String](_.title),
    typedProperty[Document, Property[User | Null], User | Null](_.user),
    typedProperty[Document, Property[js.Any | Null], js.Any | Null](_.editor),
    typedProperty[Document, ListProperty[Link], Link](_.links)
  )

  def root(): Future[Data[Document]] =
    Api.post("/service/document/documents/document/root")

  def read(id: String): Future[Data[Document]] =
    Api.get(s"/service/document/documents/document/$id")

  def list(index: Int, limit: Int, query: String = "", sorting: Seq[String] = Seq("created:desc")): Future[Table[Data[Document]]] = {
    val normalizedQuery = Option(query).map(_.trim).getOrElse("")
    val queryParameter =
      if (normalizedQuery.isEmpty) ""
      else s"&name=${encodeURIComponent(normalizedQuery)}"
    val sortParameter = renderSortParameters(sorting)

    Api.get(s"/service/document/documents?index=$index&limit=$limit$sortParameter$queryParameter")
  }

  private def renderSortParameters(sorting: Seq[String]): String = {
    val normalizedSorting = sorting.iterator.map(_.trim).filter(_.nonEmpty).toVector
    if (normalizedSorting.isEmpty) ""
    else normalizedSorting.map(value => s"&sort=${encodeURIComponent(value)}").mkString
  }
}
