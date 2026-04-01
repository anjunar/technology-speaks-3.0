package app.domain.core

import app.support.JsonModel
import jfx.core.meta.Meta
import com.anjunar.scala.enterprise.macros.PropertyMacros.makePropertyAccess
import jfx.json.JsonIgnore

import scala.scalajs.js
import scala.reflect.ClassTag

class Data[E: ClassTag](
  var data: E = null.asInstanceOf[E],
  var score: Double = 1.0d,
  @JsonIgnore
  var schema: Schema | Null = null
) extends JsonModel[Data[E]] {

  override def meta: Meta[Data[E]] = Data.meta
}

object Data {
  def meta[E : ClassTag]: Meta[Data[E]] = Meta(() => new Data())
}
