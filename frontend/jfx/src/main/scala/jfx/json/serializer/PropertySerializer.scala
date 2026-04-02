package jfx.json.serializer

import reflect.{ClassDescriptor, ParameterizedTypeDescriptor}
import jfx.core.state.Property

import scala.scalajs.js

class PropertySerializer extends Serializer[Property[?]]{

  override def serialize(input: Property[?], context: JavaContext): js.Dynamic = {

    val propertyType = context.resolvedType match {
      case parameterizedType: ParameterizedTypeDescriptor => parameterizedType.typeArguments(0)
      case _ => throw new IllegalStateException("Property must have a generic Type")
    }

    val value = input.get
    if (value == null) {
      return null.asInstanceOf[js.Dynamic]
    }

    val serializer = propertyType match {
      case cd: ClassDescriptor => SerializerFactory.build(cd.typeName)
      case _ => SerializerFactory.buildFromType(propertyType)
    }
    
    serializer.asInstanceOf[Serializer[Any]].serialize(value, context)
  }
}
