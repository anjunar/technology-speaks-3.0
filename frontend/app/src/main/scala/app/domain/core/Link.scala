package app.domain.core

import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}

import scala.scalajs.js

case class Link(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET",
  var id: String = ""
) extends JsonModel[Link] {

  override def properties: Seq[PropertyAccess[Link, ?]] = Link.properties
}

object Link {
  val properties: Seq[PropertyAccess[Link, ?]] = PropertyMacros.describeProperties[Link]
}
