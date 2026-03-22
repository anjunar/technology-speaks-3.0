package app.domain.core

import app.support.JsonModel
import jfx.core.macros.property
import jfx.core.state.PropertyAccess

import scala.scalajs.js

class Table[E](
  var rows: js.Array[E] = new js.Array[E](),
  var size: Int = 0
) extends JsonModel[Table[E]] {

  override def properties: js.Array[PropertyAccess[Table[E], ?]] =
    Table.properties[E]
}

object Table {
  def properties[E]: js.Array[PropertyAccess[Table[E], ?]] = js.Array(
    property(_.rows),
    property(_.size)
  )
}
