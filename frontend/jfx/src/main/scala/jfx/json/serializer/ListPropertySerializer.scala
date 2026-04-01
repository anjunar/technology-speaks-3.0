package jfx.json.serializer

import com.anjunar.scala.enterprise.macros.{PropertyAccess, TypeHelper}
import com.anjunar.scala.enterprise.macros.reflection.ParameterizedType
import jfx.core.state.ListProperty

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

class ListPropertySerializer extends Serializer[ListProperty[?]] {

  override def serialize(input: ListProperty[?], context: JavaContext): js.Dynamic = {
    val elemType = context.resolvedType match {
      case pt: ParameterizedType => TypeHelper.simpleRawType(pt.typeArguments(0))
      case _ => throw new IllegalStateException("ListProperty must have a generic type")
    }

    val arr = input.underlying.map { elem =>
      SerializerFactory.build(TypeHelper.rawType(elemType).asInstanceOf[Class[Any]])
        .serialize(elem, new JavaContext(elemType))
    }

    arr.asInstanceOf[js.Dynamic]
  }

}
