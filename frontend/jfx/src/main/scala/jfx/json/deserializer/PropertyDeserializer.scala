package jfx.json.deserializer

import com.anjunar.scala.enterprise.macros.TypeHelper
import com.anjunar.scala.enterprise.macros.reflection.ParameterizedType
import jfx.core.state.Property

import scala.scalajs.js.Dynamic

class PropertyDeserializer extends Deserializer[Property[?]] {

  override def deserialize(json: Dynamic, context: JsonContext): Any = {
    val propertyType = context.resolvedType match {
      case pt: ParameterizedType => TypeHelper.simpleRawType(pt.typeArguments(0))
      case _ => throw new IllegalStateException("Property must have a generic type")
    }

    val deserializer = DeserializerFactory.build(TypeHelper.rawType(propertyType).asInstanceOf[Class[Any]])
    deserializer.deserialize(json, new JsonContext(propertyType))
  }

}
