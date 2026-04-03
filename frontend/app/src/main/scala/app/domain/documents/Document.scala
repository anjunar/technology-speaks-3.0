package app.domain.documents

import app.domain.core.{AbstractEntity, Data, Link, Table, User}
import app.domain.shared.OwnerProvider
import app.support.Api
import app.support.Api.given
import jfx.core.meta.Meta
import jfx.core.state.{ListProperty, Property}
import jfx.form.validators.*

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.URIUtils.encodeURIComponent

class Document extends AbstractEntity with OwnerProvider {

  @NotBlank(message = "Titel ist erforderlich")
  val title: Property[String] = Property("")
  val bookname: Property[String] = Property("")
  val user: Property[User | Null] = Property(null)

  @NotNull(message = "Inhalt ist erforderlich")
  val editor: Property[js.Any | Null] = Property(null)

  val editable: Property[Boolean] = Property(false)

  def save(): Future[Data[Document]] =
    Api.request("/service/document/documents/document").post(this).read[Data[Document]]

  def update(): Future[Data[Document]] =
    Api.request("/service/document/documents/document").put(this).read[Data[Document]]

  def delete(): Future[Unit] =
    Api.request("/service/document/documents/document").delete(this).unit
}

object Document {
 
  def root(): Future[Data[Document]] =
    Api.request("/service/document/documents/document/root").post.read[Data[Document]]

  def read(id: String): Future[Data[Document]] =
    Api.request(s"/service/document/documents/document/$id").get.read[Data[Document]]

  def list(index: Int, limit: Int, query: String = "", sorting: Seq[String] = Seq("created:desc")): Future[Table[Data[Document]]] = {
    val normalizedQuery = Option(query).map(_.trim).getOrElse("")
    val queryParameter =
      if (normalizedQuery.isEmpty) ""
      else s"&name=${encodeURIComponent(normalizedQuery)}"
    val sortParameter = renderSortParameters(sorting)

    Api.request(s"/service/document/documents?index=$index&limit=$limit$sortParameter$queryParameter").get.read[Table[Data[Document]]]
  }

  private def renderSortParameters(sorting: Seq[String]): String = {
    val normalizedSorting = sorting.iterator.map(_.trim).filter(_.nonEmpty).toVector
    if (normalizedSorting.isEmpty) ""
    else normalizedSorting.map(value => s"&sort=${encodeURIComponent(value)}").mkString
  }
}
