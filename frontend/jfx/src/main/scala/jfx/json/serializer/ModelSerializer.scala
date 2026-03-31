package jfx.json.serializer

import com.anjunar.scala.enterprise.macros.PropertyAccess
import jfx.core.state.{ListProperty, Property, ReadOnlyProperty}
import jfx.form.Model

import scala.scalajs.js
import scala.scalajs.js.Dynamic

class ModelSerializer extends Serializer[Model[?]] {

  override def serialize(input: Model[?], context: JavaContext): Dynamic = {
    val out = js.Dictionary[js.Any]()
    input.meta.properties.foreach { access =>
      val value = access.asInstanceOf[PropertyAccess[Any, Any]].get(input)
      if (value != null) {
        val serialized = serializeValue(value)
        if (serialized != null && !js.isUndefined(serialized)) {
          out.update(access.name, serialized)
        }
      }
    }
    out.asInstanceOf[js.Dynamic]
  }

  private def serializeValue(value: Any): js.Any = {
    value match {
      case null => null
      case p: ReadOnlyProperty[?] => serializeValue(p.get)
      case m: Model[?] => serialize(m, new JavaContext(null))
      case arr: js.Array[?] => arr.map(serializeValue)
      case v => value.asInstanceOf[js.Any]
    }
  }

}
