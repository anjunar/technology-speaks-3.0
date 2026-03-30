package jfx.form.editor.plugins

import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import com.anjunar.scala.enterprise.macros.PropertyMacros.makePropertyAccess
import jfx.core.state.Property
import jfx.form.Model

import scala.scalajs.js

final case class LinkDescriptor(
    val href: Property[String] = Property(""),
    val title: Property[String] = Property("")
) extends Model[LinkDescriptor] {

  override def properties: Seq[PropertyAccess[LinkDescriptor, ?]] = LinkDescriptor.properties
}

object LinkDescriptor {
  val properties: Seq[PropertyAccess[LinkDescriptor, ?]] = PropertyMacros.describeProperties[LinkDescriptor]
}
