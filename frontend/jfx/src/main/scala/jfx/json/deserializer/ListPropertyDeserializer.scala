package jfx.json.deserializer

import com.anjunar.scala.enterprise.macros.TypeHelper
import com.anjunar.scala.enterprise.macros.reflection.ParameterizedType
import jfx.core.state.ListProperty

import scala.scalajs.js
import scala.scalajs.js.Dynamic

class ListPropertyDeserializer extends Deserializer[ListProperty[?]] {

  override def deserialize(json: Dynamic, context: JsonContext): Any = {
    val elemType = context.resolvedType match {
      case pt: ParameterizedType => TypeHelper.simpleRawType(pt.typeArguments(0))
      case _ => throw new IllegalStateException("ListProperty must have a generic type")
    }

    val arr = json.asInstanceOf[js.Array[Dynamic]]
    val result = js.Array[Any]()

    var i = 0
    while (i < arr.length) {
      val deserializer = DeserializerFactory.build(TypeHelper.rawType(elemType).asInstanceOf[Class[Any]])
      val value = deserializer.deserialize(arr(i), new JsonContext(elemType))
      result.push(value)
      i += 1
    }

    new ListProperty(result)
  }

}
