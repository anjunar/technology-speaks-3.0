package jfx.form.editor.plugins

import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import com.anjunar.scala.enterprise.macros.PropertyMacros.makePropertyAccess
import jfx.core.state.Property
import jfx.form.Model

import scala.scalajs.js

final case class Dimensions(
    val image: Property[String] = Property(""),
    val width: Property[Double] = Property(0.0),
    val height: Property[Double] = Property(0.0)
) extends Model[Dimensions] {

  override def properties: Seq[PropertyAccess[Dimensions, ?]] = Dimensions.properties
}

object Dimensions {
  val properties: Seq[PropertyAccess[Dimensions, ?]] = PropertyMacros.describeProperties[Dimensions]
}
