package app.domain.documents

import app.domain.core.AbstractLink
import app.support.JsonModel
import jfx.core.meta.Meta

import scala.scalajs.js

class DocumentsLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[DocumentsLink] with AbstractLink {

  override def meta: Meta[DocumentsLink] = DocumentsLink.meta

  override def name: String = "Dokument"

  override def icon: String = "edit_document"
}

object DocumentsLink {
  val meta : Meta[DocumentsLink] = Meta()}
