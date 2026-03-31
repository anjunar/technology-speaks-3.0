package jfx.form.editor.plugins

import jfx.core.meta.Meta
import com.anjunar.scala.enterprise.macros.PropertyMacros.makePropertyAccess
import jfx.core.state.Property
import jfx.form.Model

import scala.scalajs.js

final case class Dimensions(
    val image: Property[String] = Property(""),
    val width: Property[Double] = Property(0.0),
    val height: Property[Double] = Property(0.0)
) extends Model[Dimensions] {

  override def meta: Meta[Dimensions] = Dimensions.meta
}

object Dimensions {
  val meta: Meta[Dimensions] = Meta()
}
