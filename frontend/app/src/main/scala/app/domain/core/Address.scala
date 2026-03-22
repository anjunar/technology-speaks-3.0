package app.domain.core

import jfx.core.macros.property
import jfx.core.state.{ListProperty, Property, PropertyAccess}

import java.util.UUID
import scala.scalajs.js

class Address(
               var id: Property[UUID] = Property(null),
               var modified: Property[String] = Property(""),
               var created: Property[String] = Property(""),
               var street: Property[String] = Property(""),
               var number: Property[String] = Property(""),
               var zipCode: Property[String] = Property(""),
               var country: Property[String] = Property(""),
               var links: ListProperty[Link] = ListProperty()
) extends AbstractEntity[Address] {

  override def properties: js.Array[PropertyAccess[Address, ?]] =
    Address.properties
}

object Address {
  val properties: js.Array[PropertyAccess[Address, ?]] = js.Array(
    property(_.id),
    property(_.modified),
    property(_.created),
    property(_.street),
    property(_.number),
    property(_.zipCode),
    property(_.country),
    property(_.links)
  )
}
