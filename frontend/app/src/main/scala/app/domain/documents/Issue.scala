package app.domain.documents

import app.domain.core.{AbstractEntity, Data, Link, Table, User}
import app.domain.shared.OwnerProvider
import app.support.Api
import app.support.Api.given
import jfx.core.meta.Meta
import jfx.core.state.{ListProperty, Property}
import jfx.form.validators.{NotBlank, NotNull}

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js

class Issue extends AbstractEntity[Issue] with OwnerProvider {

  @NotBlank(message = "Titel ist erforderlich")
  val title: Property[String] = Property("")
  val user: Property[User | Null] = Property(null)

  @NotNull(message = "Inhalt ist erforderlich")
  val editor: Property[js.Any | Null] = Property(null)

  val editable: Property[Boolean] = Property(false)
}

object Issue {
 
  def read(id: String): Future[Data[Issue]] =
    Api.get(s"/service/document/documents/document/$id/issues/issue").map(raw => Api.deserialize[Data[Issue]](raw))

  def read(documentId: String, issueId: String): Future[Data[Issue]] =
    Api.get(s"/service/document/documents/document/$documentId/issues/issue/$issueId").map(raw => Api.deserialize[Data[Issue]](raw))

  def list(index: Int, limit: Int, document: Document): Future[Table[Data[Issue]]] =
    Api.get(s"/service/document/documents/document/${document.id.get}/issues?index=$index&limit=$limit&sort=created:desc").map(raw => Api.deserialize[Table[Data[Issue]]](raw))
}
