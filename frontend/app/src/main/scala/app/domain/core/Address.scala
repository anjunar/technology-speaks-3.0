package app.domain.core

import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{ListProperty, Property, PropertyAccess}

import java.util.UUID
import scala.scalajs.js

class Address(
               val id: Property[UUID] = Property(null),
               val modified: Property[String] = Property(""),
               val created: Property[String] = Property(""),
               val street: Property[String] = Property(""),
               val number: Property[String] = Property(""),
               val zipCode: Property[String] = Property(""),
               val country: Property[String] = Property(""),
               val links: ListProperty[Link] = ListProperty()
) extends AbstractEntity[Address] {

  override def properties: js.Array[PropertyAccess[Address, ?]] =
    Address.properties
}

object Address {
  val properties: js.Array[PropertyAccess[Address, ?]] = js.Array(
    typedProperty[Address, Property[UUID], UUID](_.id),
    typedProperty[Address, Property[String], String](_.modified),
    typedProperty[Address, Property[String], String](_.created),
    typedProperty[Address, Property[String], String](_.street),
    typedProperty[Address, Property[String], String](_.number),
    typedProperty[Address, Property[String], String](_.zipCode),
    typedProperty[Address, Property[String], String](_.country),
    typedProperty[Address, ListProperty[Link], Link](_.links)
  )
}
