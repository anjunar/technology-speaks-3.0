package jfx.json.serializer

import reflect.{ClassDescriptor, ParameterizedTypeDescriptor}
import jfx.core.state.ListProperty

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

class ListPropertySerializer extends Serializer[ListProperty[?]] {

  override def serialize(input: ListProperty[?], context: JavaContext): js.Dynamic = {
    val elemType = context.resolvedType match {
      case pt: ParameterizedTypeDescriptor => pt.typeArguments(0)
      case _ => throw new IllegalStateException("ListProperty must have a generic type")
    }

    val serializer = elemType match {
      case cd: ClassDescriptor => SerializerFactory.build(cd.typeName)
      case _ => SerializerFactory.buildFromType(elemType)
    }
    
    val arr = input.underlying.map { elem =>
      serializer.asInstanceOf[Serializer[Any]].serialize(elem, context)
    }

    arr.asInstanceOf[js.Dynamic]
  }

}
