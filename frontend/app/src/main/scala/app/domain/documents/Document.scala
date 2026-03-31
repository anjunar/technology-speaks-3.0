package app.domain.documents

import app.domain.core.{AbstractEntity, Data, Link, Table, User}
import app.domain.shared.OwnerProvider
import app.support.Api
import jfx.core.meta.Meta
import com.anjunar.scala.enterprise.macros.validation.{NotBlank, NotNull}
import jfx.core.state.{ListProperty, Property}

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.URIUtils.encodeURIComponent

class Document extends AbstractEntity[Document] with OwnerProvider {

  @NotBlank(message = "Titel ist erforderlich")
  val title: Property[String] = Property("")
  val bookname: Property[String] = Property("")
  val user: Property[User | Null] = Property(null)

  @NotNull(message = "Inhalt ist erforderlich")
  val editor: Property[js.Any | Null] = Property(null)

  val editable: Property[Boolean] = Property(false)

  override def meta: Meta[Document] = Document.meta

  def save(): Future[Data[Document]] =
    Api.post("/service/document/documents/document", this)

  def update(): Future[Data[Document]] =
    Api.put("/service/document/documents/document", this)

  def delete(): Future[Unit] =
    Api.delete("/service/document/documents/document", this)
}

object Document {
  val meta: Meta[Document] = Meta()
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
