package app.domain.documents

import app.domain.core.AbstractLink
import app.support.JsonModel
import jfx.core.meta.Meta
import jfx.json.JsonType

import scala.scalajs.js

@JsonType("document-root")
class DocumentsLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends AbstractLink {
  
  override def name: String = "Dokument"

  override def icon: String = "edit_document"
}