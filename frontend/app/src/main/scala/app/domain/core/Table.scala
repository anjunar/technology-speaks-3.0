package app.domain.core

import app.support.JsonModel
import jfx.core.meta.Meta
import com.anjunar.scala.enterprise.macros.PropertyMacros.makePropertyAccess
import jfx.json.JsonIgnore

import scala.scalajs.js
import scala.reflect.ClassTag

class Table[E: ClassTag](
  var rows: js.Array[E] = new js.Array[E](),
  var size: Int = 0,
  @JsonIgnore
  var schema: Schema | Null = null
) extends JsonModel[Table[E]] {

  override def meta: Meta[Table[E]] = Table.meta
}

object Table {
  def meta[E : ClassTag]: Meta[Table[E]] = Meta()
}
