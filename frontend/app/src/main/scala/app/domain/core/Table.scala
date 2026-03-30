package app.domain.core

import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.{PropertyMacros, PropertyAccess}
import com.anjunar.scala.enterprise.macros.PropertyMacros.makePropertyAccess

import scala.scalajs.js
import scala.reflect.ClassTag

class Table[E: ClassTag](
  var rows: js.Array[E] = new js.Array[E](),
  var size: Int = 0,
  var schema: Schema | Null = null
) extends JsonModel[Table[E]] {

  override def properties: Seq[PropertyAccess[Table[E], ?]] =
    Table.properties[E]
}

object Table {
  def properties[E: ClassTag]: Seq[PropertyAccess[Table[E], ?]] = Seq(
    makePropertyAccess[Table[E], js.Array[E]](_.rows),
    makePropertyAccess[Table[E], Int](_.size),
    makePropertyAccess[Table[E], Schema | Null](_.schema)
  )
}
