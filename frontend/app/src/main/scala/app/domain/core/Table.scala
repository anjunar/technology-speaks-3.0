package app.domain.core

import app.support.JsonModel
import jfx.core.macros.property
import jfx.core.state.PropertyAccess

import scala.scalajs.js
import scala.reflect.ClassTag

class Table[E: ClassTag](
  var rows: js.Array[E] = new js.Array[E](),
  var size: Int = 0,
  var schema: Schema | Null = null
) extends JsonModel[Table[E]] {

  override def properties: js.Array[PropertyAccess[Table[E], ?]] =
    Table.properties[E]
}

object Table {
  def properties[E: ClassTag]: js.Array[PropertyAccess[Table[E], ?]] = js.Array(
    property(_.rows),
    property(_.size),
    property(_.schema)
  )
}
