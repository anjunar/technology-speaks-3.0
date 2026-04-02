package jfx.form.editor.plugins

import jfx.core.state.Property
import jfx.form.Model

import scala.scalajs.js

final case class Dimensions(
    val image: Property[String] = Property(""),
    val width: Property[Double] = Property(0.0),
    val height: Property[Double] = Property(0.0)
) extends Model[Dimensions]

object Dimensions {

}
