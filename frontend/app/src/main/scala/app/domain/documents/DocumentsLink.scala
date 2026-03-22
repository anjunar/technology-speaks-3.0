package app.domain.documents

import app.domain.core.AbstractLink
import app.support.JsonModel
import jfx.core.macros.property
import jfx.core.state.PropertyAccess

import scala.scalajs.js

class DocumentsLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[DocumentsLink] with AbstractLink {

  override def properties: js.Array[PropertyAccess[DocumentsLink, ?]] =
    DocumentsLink.properties

  override def name: String = "Dokument"

  override def icon: String = "edit_document"
}

object DocumentsLink {
  val properties: js.Array[PropertyAccess[DocumentsLink, ?]] = js.Array(
    property(_.rel),
    property(_.url),
    property(_.method)
  )
}
