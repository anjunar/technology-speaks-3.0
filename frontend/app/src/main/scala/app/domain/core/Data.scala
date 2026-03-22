package app.domain.core

import app.support.JsonModel
import jfx.core.macros.property
import jfx.core.state.PropertyAccess

import scala.scalajs.js

class Data[E](
  var data: E = null.asInstanceOf[E]
) extends JsonModel[Data[E]] {

  override def properties: js.Array[PropertyAccess[Data[E], ?]] =
    Data.properties[E]
}

object Data {
  def properties[E]: js.Array[PropertyAccess[Data[E], ?]] = js.Array(
    property(_.data)
  )
}
