package jfx.json.deserializer

import com.anjunar.scala.enterprise.macros.reflection.ParameterizedType
import jfx.core.state.ListProperty

import scala.scalajs.js
import scala.scalajs.js.Dynamic

class ListPropertyDeserializer extends Deserializer[ListProperty[?]] {

  override def deserialize(json: Dynamic, context: JsonContext): Any = {
    val elemType = context.resolvedType match {
      case pt: ParameterizedType => pt.typeArguments(0)
      case _ => throw new IllegalStateException("ListProperty must have a generic type")
    }

    val arr = json.asInstanceOf[js.Array[Dynamic]]
    val result = js.Array[Any]()

    var i = 0
    while (i < arr.length) {
      val elemJson = arr(i)
      val elemContext = resolveElementType(elemJson, elemType)
      val deserializer = DeserializerFactory.buildFromType(elemContext.resolvedType)
      val value = deserializer.deserialize(elemJson, elemContext)
      result.push(value)
      i += 1
    }

    new ListProperty(result)
  }

  private def resolveElementType(json: Dynamic, declaredType: com.anjunar.scala.enterprise.macros.reflection.Type): JsonContext = {
    val jsonType = readJsonType(json)
    jsonType match {
      case Some(typeName) =>
        com.anjunar.scala.enterprise.macros.MetaClassLoader.getByTypeName(typeName) match {
          case Some(clazz) => new JsonContext(clazz)
          case None => new JsonContext(declaredType)
        }
      case None => new JsonContext(declaredType)
    }
  }

  private def readJsonType(json: Dynamic): Option[String] = {
    val rawType = json.selectDynamic("@type").asInstanceOf[js.Any]
    if (rawType == null || js.isUndefined(rawType)) None
    else Some(rawType.toString)
  }

}
