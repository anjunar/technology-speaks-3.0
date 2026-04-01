package jfx.json.deserializer

import com.anjunar.scala.enterprise.macros.reflection.{ParameterizedType, Type}
import jfx.core.state.Property

import scala.scalajs.js.Dynamic

class PropertyDeserializer extends Deserializer[Property[?]] {

  override def deserialize(json: Dynamic, context: JsonContext): Any = {
    val propertyType = context.resolvedType match {
      case pt: ParameterizedType =>
        if (pt.typeArguments.isEmpty) throw new IllegalStateException("Property must have a generic type argument")
        pt.typeArguments(0).asInstanceOf[Type]
      case _ => throw new IllegalStateException("Property must have a generic type")
    }

    val deserializer = DeserializerFactory.buildFromType(propertyType)
    deserializer.deserialize(json, new JsonContext(propertyType))
  }

}
