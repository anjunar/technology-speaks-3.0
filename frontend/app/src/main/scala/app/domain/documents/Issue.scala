package app.domain.documents

import app.domain.core.{AbstractEntity, Data, Link, Table, User}
import app.domain.shared.OwnerProvider
import app.support.Api
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import com.anjunar.scala.enterprise.macros.validation.{NotBlank, NotNull}
import jfx.core.state.{ListProperty, Property}

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

  override def properties: Seq[PropertyAccess[Issue, ?]] = Issue.properties
}

object Issue {
  val properties: Seq[PropertyAccess[Issue, ?]] = PropertyMacros.describeProperties[Issue]
  def read(id: String): Future[Data[Issue]] =
    Api.get(s"/service/document/documents/document/$id/issues/issue")

  def read(documentId: String, issueId: String): Future[Data[Issue]] =
    Api.get(s"/service/document/documents/document/$documentId/issues/issue/$issueId")

  def list(index: Int, limit: Int, document: Document): Future[Table[Data[Issue]]] =
    Api.get(s"/service/document/documents/document/${document.id.get}/issues?index=$index&limit=$limit&sort=created:desc")
}
