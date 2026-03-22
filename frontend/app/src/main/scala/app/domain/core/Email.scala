package app.domain.core

import jfx.core.macros.property
import jfx.core.state.{ListProperty, Property, PropertyAccess}

import scala.scalajs.js

class Email(
  var id: Property[String] = Property(""),
  var modified: Property[String] = Property(""),
  var created: Property[String] = Property(""),
  var value: Property[String] = Property(""),
  var links: ListProperty[Link] = ListProperty()
) extends AbstractEntity[Email] {

  override def properties: js.Array[PropertyAccess[Email, ?]] =
    Email.properties
}

object Email {
  val properties: js.Array[PropertyAccess[Email, ?]] = js.Array(
    property(_.id),
    property(_.modified),
    property(_.created),
    property(_.value),
    property(_.links)
  )
}
