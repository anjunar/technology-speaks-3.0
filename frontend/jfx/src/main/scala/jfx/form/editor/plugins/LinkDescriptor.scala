package jfx.form.editor.plugins

import jfx.core.meta.Meta
import com.anjunar.scala.enterprise.macros.PropertyMacros.makePropertyAccess
import jfx.core.state.Property
import jfx.form.Model

import scala.scalajs.js

final case class LinkDescriptor(
    val href: Property[String] = Property(""),
    val title: Property[String] = Property("")
) extends Model[LinkDescriptor] {

  override def meta: Meta[LinkDescriptor] = LinkDescriptor.meta
}

object LinkDescriptor {
  val meta: Meta[LinkDescriptor] = Meta(() => new LinkDescriptor())
}
