package jfx.json.serializer

import com.anjunar.scala.enterprise.macros.TypeHelper
import com.anjunar.scala.enterprise.macros.reflection.ParameterizedType
import jfx.core.state.Property

import scala.scalajs.js

class PropertySerializer extends Serializer[Property[?]]{

  override def serialize(input: Property[?], context: JavaContext): js.Dynamic = {

    val propertyType = context.resolvedType match {
      case parameterizedType: ParameterizedType => TypeHelper.simpleRawType(parameterizedType.typeArguments(0))
      case _ => throw new IllegalStateException("Property must have a generic Type")
    }

    val value = input.get
    if (value == null) {
      return null.asInstanceOf[js.Dynamic]
    }

    SerializerFactory.build(propertyType.runtimeClass.asInstanceOf[Class[Any]])
      .serialize(value, new JavaContext(propertyType))
  }
}
