package jfx.json.serializer

import reflect.{ClassDescriptor, ParameterizedTypeDescriptor}
import jfx.json.deserializer.JsonContext

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

class JsArraySerializer extends Serializer[js.Array[?]] {

  override def serialize(input: js.Array[?], context: JavaContext): js.Dynamic = {
    val elemType = context.resolvedType match {
      case pt: ParameterizedTypeDescriptor => pt.typeArguments(0)
      case _ => throw new IllegalStateException("js.Array must have a generic type")
    }

    val serializer = elemType match {
      case cd: ClassDescriptor => SerializerFactory.build(cd.typeName)
      case _ => SerializerFactory.buildFromType(elemType)
    }
    
    val arr = input.map { elem =>
      serializer.asInstanceOf[Serializer[Any]].serialize(elem, context)
    }

    arr.asInstanceOf[js.Dynamic]
  }

}
