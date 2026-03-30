package app.domain.documents

import app.domain.core.AbstractLink
import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}

import scala.scalajs.js

class DocumentsLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[DocumentsLink] with AbstractLink {

  override def properties: Seq[PropertyAccess[DocumentsLink, ?]] = DocumentsLink.properties

  override def name: String = "Dokument"

  override def icon: String = "edit_document"
}

object DocumentsLink {
  val properties: Seq[PropertyAccess[DocumentsLink, ?]]= PropertyMacros.describeProperties[DocumentsLink]}
