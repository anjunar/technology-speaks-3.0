package jfx.form.editor.plugins

import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{Property, PropertyAccess}
import jfx.form.Model

import scala.scalajs.js

class Dimensions(
  val image: Property[String] = Property(""),
  val width: Property[Double] = Property(320.0),
  val height: Property[Double] = Property(240.0)
) extends Model[Dimensions] {

  override def properties: js.Array[PropertyAccess[Dimensions, ?]] =
    Dimensions.properties
}

object Dimensions {
  val properties: js.Array[PropertyAccess[Dimensions, ?]] = js.Array(
    typedProperty[Dimensions, Property[String], String](_.image),
    typedProperty[Dimensions, Property[Double], Double](_.width),
    typedProperty[Dimensions, Property[Double], Double](_.height)
  )
}
