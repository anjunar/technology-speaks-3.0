package jfx.form.editor.plugins

import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{Property, PropertyAccess}
import jfx.form.Model

import scala.scalajs.js

class LinkDescriptor(
  val href: Property[String] = Property(""),
  val title: Property[String] = Property("")
) extends Model[LinkDescriptor] {

  override def properties: js.Array[PropertyAccess[LinkDescriptor, ?]] =
    LinkDescriptor.properties
}

object LinkDescriptor {
  val properties: js.Array[PropertyAccess[LinkDescriptor, ?]] = js.Array(
    typedProperty[LinkDescriptor, Property[String], String](_.href),
    typedProperty[LinkDescriptor, Property[String], String](_.title)
  )
}
