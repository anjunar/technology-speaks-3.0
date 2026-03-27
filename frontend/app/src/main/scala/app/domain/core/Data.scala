package app.domain.core

import app.support.JsonModel
import jfx.core.macros.property
import jfx.core.state.PropertyAccess

import scala.scalajs.js
import scala.reflect.ClassTag

class Data[E: ClassTag](
  var data: E = null.asInstanceOf[E],
  var score: Double = 1.0d,
  var schema: Schema | Null = null
) extends JsonModel[Data[E]] {

  override def properties: js.Array[PropertyAccess[Data[E], ?]] =
    Data.properties[E]
}

object Data {
  def properties[E: ClassTag]: js.Array[PropertyAccess[Data[E], ?]] = js.Array(
    property(_.data),
    property(_.score),
    property(_.schema)
  )
}
