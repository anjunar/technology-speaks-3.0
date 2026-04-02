package jfx.json.serializer

import reflect.macros.PropertySupport
import reflect.{ClassDescriptor, PropertyAccessor, PropertyDescriptor, TypeDescriptor}
import reflect.ReflectRegistry
import jfx.core.state.{ListProperty, Property, ReadOnlyProperty}
import jfx.form.Model
import jfx.json.{JsonHelpers, JsonMapper}

import scala.scalajs.js
import scala.scalajs.js.Dynamic

class ModelSerializer extends Serializer[Model[?]] {

  override def serialize(input: Model[?], context: JavaContext): Dynamic = {
    val out = js.Dictionary[js.Any]()
    getJsonType(input).foreach(t => out.update("@type", t))

    val properties = context.resolvedType.properties
    properties.foreach { prop =>
      if (!JsonHelpers.isIgnored(prop)) {
        val value = readPropertyValue(input, context.resolvedType.typeName, prop)
        prop.propertyType match {
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

  private def isMapType(tpe: TypeDescriptor): Boolean = tpe match {
    case pt: reflect.ParameterizedTypeDescriptor => pt.rawType.typeName == "scala.collection.immutable.Map" || pt.rawType.typeName == "Map"
    case cd: reflect.ClassDescriptor => cd.typeName == "scala.collection.immutable.Map" || cd.typeName == "Map"
    case _ => false
  }

  private def readPropertyValue(model: Model[?], typeName: String, prop: PropertyDescriptor): Any = {
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

  private def getJsonType(model: Model[?]): Option[String] = {
    Some(model.getClass.getSimpleName.stripSuffix("$"))
  }

  private def serializeValue(value: Any): js.Any = value match {
    case null => null
    case p: Property[?] => serializeValue(p.get)
    case p: ReadOnlyProperty[?] => serializeValue(p.get)
    case Some(v) => serializeValue(v)
    case None => null
    case m: Model[?] => JsonMapper.serialize(m)
    case arr: js.Array[?] => arr.map(serializeValue)
    case map: Map[?, ?] =>
      val out = js.Dictionary[js.Any]()
      map.foreach { case (k, v) => out.update(k.toString, serializeValue(v)) }
      out.asInstanceOf[js.Dynamic]
    case v => v.asInstanceOf[js.Any]
  }
}
