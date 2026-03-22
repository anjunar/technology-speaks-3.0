package jfx.form.editor.plugins

import jfx.core.macros.property
import jfx.core.state.{Property, PropertyAccess}
import jfx.form.Model

import scala.scalajs.js

class Dimensions(
  var image: Property[String] = Property(""),
  var width: Property[Double] = Property(320.0),
  var height: Property[Double] = Property(240.0)
) extends Model[Dimensions] {

  override def properties: js.Array[PropertyAccess[Dimensions, ?]] =
    Dimensions.properties
}

object Dimensions {
  val properties: js.Array[PropertyAccess[Dimensions, ?]] = js.Array(
    property(_.image),
    property(_.width),
    property(_.height)
  )
}
