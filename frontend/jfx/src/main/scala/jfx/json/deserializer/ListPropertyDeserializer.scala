package jfx.json.deserializer

import reflect.{ClassDescriptor, ParameterizedTypeDescriptor, TypeDescriptor}
import jfx.core.state.ListProperty

import scala.scalajs.js
import scala.scalajs.js.Dynamic

class ListPropertyDeserializer extends Deserializer[ListProperty[?]] {

  override def deserialize(json: Dynamic, context: JsonContext): Any = {
    val elemType = context.resolvedType match {
      case pt: ParameterizedTypeDescriptor => pt.typeArguments(0)
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

  private def resolveElementType(json: Dynamic, declaredType: TypeDescriptor): JsonContext = {
    val jsonType = readJsonType(json)
    jsonType match {
      case Some(typeName) =>
        reflect.ReflectRegistry.factoriesByTypeName.get(typeName) match {
          case Some(clazz) => new JsonContext(clazz)
          case None => new JsonContext(declaredType)
        }
      case None =>
        declaredType match {
          case cd: ClassDescriptor if cd.baseTypes.nonEmpty =>
            findConcreteSubType(cd, json) match {
              case Some(concreteType) => new JsonContext(concreteType)
              case None => new JsonContext(declaredType)
            }
          case _ => new JsonContext(declaredType)
        }
    }
  }

  private def findConcreteSubType(declaredType: ClassDescriptor, json: Dynamic): Option[ClassDescriptor] = {
    val jsonType = readJsonType(json)
    jsonType.flatMap(typeName => reflect.ReflectRegistry.factoriesByTypeName.get(typeName))
  }

  private def readJsonType(json: Dynamic): Option[String] = {
    val rawType = json.selectDynamic("@type").asInstanceOf[js.Any]
    if (rawType == null || js.isUndefined(rawType)) None
    else Some(rawType.toString)
  }

}
