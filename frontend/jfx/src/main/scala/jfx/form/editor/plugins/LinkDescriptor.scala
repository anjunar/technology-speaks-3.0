package jfx.form.editor.plugins

import jfx.core.macros.property
import jfx.core.state.{Property, PropertyAccess}
import jfx.form.Model

import scala.scalajs.js

class LinkDescriptor(
  var href: Property[String] = Property(""),
  var title: Property[String] = Property("")
) extends Model[LinkDescriptor] {

  override def properties: js.Array[PropertyAccess[LinkDescriptor, ?]] =
    LinkDescriptor.properties
}

object LinkDescriptor {
  val properties: js.Array[PropertyAccess[LinkDescriptor, ?]] = js.Array(
    property(_.href),
    property(_.title)
  )
}
