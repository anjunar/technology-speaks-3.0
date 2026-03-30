package app.domain.security

import app.domain.core.AbstractLink
import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}

import scala.scalajs.js

class ConfirmLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends JsonModel[ConfirmLink] with AbstractLink {

  override def properties: Seq[PropertyAccess[ConfirmLink, ?]] = ConfirmLink.properties

  override def name: String = "Bestaetigen"

  override def icon: String = "approval"
}

object ConfirmLink {
  val properties: Seq[PropertyAccess[ConfirmLink, ?]]= PropertyMacros.describeProperties[ConfirmLink]}
