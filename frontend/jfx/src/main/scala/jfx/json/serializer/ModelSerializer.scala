package jfx.json.serializer

import reflect.macros.PropertySupport
import reflect.{ClassDescriptor, ParameterizedTypeDescriptor, PropertyAccessor, PropertyDescriptor, TypeDescriptor}
import reflect.ReflectRegistry
import jfx.core.state.{ListProperty, Property, ReadOnlyProperty}
import jfx.json.{JsonHelpers, JsonMapper}

import scala.scalajs.js
import scala.scalajs.js.Dynamic

class ModelSerializer extends Serializer[AnyRef] {

  override def serialize(input: AnyRef, context: JavaContext): Dynamic = {
    val out = js.Dictionary[js.Any]()
    getJsonType(input).foreach(t => out.update("@type", t))

    val (rawType, typeArgs) = context.resolvedType match {
      case pt: ParameterizedTypeDescriptor => (pt.rawType, pt.typeArguments)
      case cd: ClassDescriptor => (cd, Array.empty[TypeDescriptor])
    }

    val properties = rawType.properties
    properties.foreach { prop =>
      if (!JsonHelpers.isIgnored(prop)) {
        val value = readPropertyValue(input, rawType.typeName, prop)
        val resolvedPropertyType = resolvePropertyType(prop.propertyType, rawType, typeArgs)
        resolvedPropertyType match {
          case pt: reflect.ParameterizedTypeDescriptor if isMapType(pt) =>
            value.asInstanceOf[Map[?, ?]].foreach { case (k, v) =>
              out.update(k.toString, serializeValue(v))
            }
          case _ =>
            out.update(JsonHelpers.getJsonFieldName(prop), serializeValue(value))
        }
      }
    }
    out.asInstanceOf[js.Dynamic]
  }

  private def resolvePropertyType(propType: TypeDescriptor, rawType: ClassDescriptor, typeArgs: Array[TypeDescriptor]): TypeDescriptor = {
    propType match {
      case cd: ClassDescriptor if rawType.typeParameters.contains(cd.typeName) && typeArgs.nonEmpty =>
        val typeParamIndex = rawType.typeParameters.indexOf(cd.typeName)
        if (typeParamIndex >= 0 && typeParamIndex < typeArgs.length) typeArgs(typeParamIndex)
        else propType
      case pt: ParameterizedTypeDescriptor =>
        val resolvedArgs = pt.typeArguments.map(arg => resolvePropertyType(arg, rawType, typeArgs))
        pt.copy(typeArguments = resolvedArgs)
      case tv: reflect.TypeVariableDescriptor =>
        if rawType.typeParameters.contains(tv.name) && typeArgs.nonEmpty then
          val typeParamIndex = rawType.typeParameters.indexOf(tv.name)
          if (typeParamIndex >= 0 && typeParamIndex < typeArgs.length) typeArgs(typeParamIndex)
          else propType
        else propType
      case _ => propType
    }
  }

  private def isMapType(tpe: TypeDescriptor): Boolean = tpe match {
    case pt: reflect.ParameterizedTypeDescriptor => pt.rawType.typeName == "scala.collection.immutable.Map" || pt.rawType.typeName == "Map"
    case cd: reflect.ClassDescriptor => cd.typeName == "scala.collection.immutable.Map" || cd.typeName == "Map"
    case _ => false
  }

  private def readPropertyValue(model: AnyRef, typeName: String, prop: PropertyDescriptor): Any = {
    ReflectRegistry.getPropertyAccessor(typeName, prop.name) match {
      case Some(accessor) =>
        val value = accessor.get(model)
        value match {
          case p: Property[?] => p.get
          case p: ReadOnlyProperty[?] => p.get
          case other => other
        }
      case None =>
        null
    }
  }

  private def getJsonType(model: Any): Option[String] = {
    Some(model.getClass.getSimpleName.stripSuffix("$"))
  }

  private def serializeValue(value: Any): js.Any = value match {
    case null => null
    case p: Property[?] => serializeValue(p.get)
    case p: ReadOnlyProperty[?] => serializeValue(p.get)
    case Some(v) => serializeValue(v)
    case None => null
    case arr: js.Array[?] => arr.map(serializeValue)
    case map: Map[?, ?] =>
      val out = js.Dictionary[js.Any]()
      map.foreach { case (k, v) => out.update(k.toString, serializeValue(v)) }
      out.asInstanceOf[js.Dynamic]
    case v: String => v
    case v: Int => v
    case v: Double => v
    case v: Float => v
    case v: Long => v
    case v: Boolean => v
    case v => JsonMapper.serialize(v)
  }
}
