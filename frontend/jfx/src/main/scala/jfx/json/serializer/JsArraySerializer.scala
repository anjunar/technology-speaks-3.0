package jfx.json.serializer

import com.anjunar.scala.enterprise.macros.{PropertyAccess, TypeHelper}
import com.anjunar.scala.enterprise.macros.reflection.ParameterizedType
import jfx.json.deserializer.JsonContext

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

class JsArraySerializer extends Serializer[js.Array[?]] {

  override def serialize(input: js.Array[?], context: JavaContext): js.Dynamic = {
    val elemType = context.resolvedType match {
      case pt: ParameterizedType => TypeHelper.simpleRawType(pt.typeArguments(0))
      case _ => throw new IllegalStateException("js.Array must have a generic type")
    }

    val arr = input.map { elem =>
      SerializerFactory.build(TypeHelper.rawType(elemType).asInstanceOf[Class[Any]])
        .serialize(elem, new JavaContext(elemType))
    }

    arr.asInstanceOf[js.Dynamic]
  }

}
