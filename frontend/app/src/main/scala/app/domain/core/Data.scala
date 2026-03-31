package app.domain.core

import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.{PropertyMacros, PropertyAccess}
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

  override def properties: Seq[PropertyAccess[Data[E], ?]] =
    Data.properties[E]
}

object Data {
  def properties[E: ClassTag]: Seq[PropertyAccess[Data[E], ?]] = Seq(
    makePropertyAccess[Data[E], E](_.data),
    makePropertyAccess[Data[E], Double](_.score),
    makePropertyAccess[Data[E], Schema | Null](_.schema)
  )
}
