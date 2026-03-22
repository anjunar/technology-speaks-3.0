package app.domain.core

import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{ListProperty, Property, PropertyAccess}

import java.util.UUID
import scala.scalajs.js

class Email(
             val id: Property[UUID] = Property(null),
             val modified: Property[String] = Property(""),
             val created: Property[String] = Property(""),
             val value: Property[String] = Property(""),
             val links: ListProperty[Link] = ListProperty()
) extends AbstractEntity[Email] {

  override def properties: js.Array[PropertyAccess[Email, ?]] =
    Email.properties
}

object Email {
  val properties: js.Array[PropertyAccess[Email, ?]] = js.Array(
    typedProperty[Email, Property[UUID], UUID](_.id),
    typedProperty[Email, Property[String], String](_.modified),
    typedProperty[Email, Property[String], String](_.created),
    typedProperty[Email, Property[String], String](_.value),
    typedProperty[Email, ListProperty[Link], Link](_.links)
  )
}
