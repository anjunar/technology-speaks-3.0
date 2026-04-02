package jfx.json.deserializer

import reflect.{ParameterizedTypeDescriptor, TypeDescriptor}
import jfx.core.state.Property

import scala.scalajs.js
import scala.scalajs.js.Dynamic

class PropertyDeserializer extends Deserializer[Property[?]] {

  override def deserialize(json: Dynamic, context: JsonContext): Any = {
    val propertyType = context.resolvedType match {
      case pt: ParameterizedTypeDescriptor =>
        if (pt.typeArguments.isEmpty) throw new IllegalStateException("Property must have a generic type argument")
        pt.typeArguments(0).asInstanceOf[TypeDescriptor]
      case _ => throw new IllegalStateException("Property must have a generic type")
    }

    // Check if this is an Option type (Property[Option[T]])
    if (isOptionType(propertyType)) {
      if (json == null || js.isUndefined(json)) {
        None
      } else {
        // Get element type from Option[T]
        val elementType = propertyType match {
          case pt: ParameterizedTypeDescriptor if pt.typeArguments.nonEmpty => pt.typeArguments(0)
          case _ => propertyType
        }
        val deserializer = DeserializerFactory.buildFromType(elementType)
        val value = deserializer.deserialize(json, new JsonContext(elementType))
        Some(value)
      }
    } else {
      val deserializer = DeserializerFactory.buildFromType(propertyType)
      deserializer.deserialize(json, new JsonContext(propertyType))
    }
  }

  private def isOptionType(tpe: TypeDescriptor): Boolean = {
    tpe.typeName == "scala.Option" || tpe.typeName == "Option"
  }

}
