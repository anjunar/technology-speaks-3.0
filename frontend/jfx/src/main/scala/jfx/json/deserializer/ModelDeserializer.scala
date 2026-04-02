package jfx.json.deserializer

import reflect.macros.PropertySupport
import reflect.{PropertyAccessor, TypeDescriptor}
import jfx.core.state.{ListProperty, Property}
import jfx.form.Model
import jfx.json.JsonHelpers

import scala.scalajs.js
import scala.scalajs.js.Dynamic

class ModelDeserializer extends Deserializer[Model[?]] {

  override def deserialize(json: Dynamic, context: JsonContext): Any = {
    val modelType = context.resolvedType match {
      case cd: reflect.ClassDescriptor => cd
      case pt: reflect.ParameterizedTypeDescriptor => pt.rawType
      case _ => throw new IllegalArgumentException(s"Expected ClassDescriptor, got ${context.resolvedType}")
    }

    modelType.typeName match {
      case "scala.scalajs.js.Array" =>
        val deserializer = new JsArrayDeserializer()
        return deserializer.deserialize(json, context)
      case "scala.collection.immutable.Map" | "Map" =>
        context.resolvedType match {
          case pt: reflect.ParameterizedTypeDescriptor => return deserializeMap(json, pt)
          case _ => throw new IllegalArgumentException("Map must be parameterized")
        }
      case _ =>
    }

    val factory = JsonHelpers.findFactory(readJsonType(json), modelType)
    val model = factory().asInstanceOf[Model[?]]
    populateModel(model, json)
    model
  }

  private def readJsonType(json: Dynamic): Option[String] =
    Option(json.selectDynamic("@type").asInstanceOf[js.Any])
      .filter(v => v != null && !js.isUndefined(v))
      .map(_.toString)

  private def populateModel(model: Model[?], json: Dynamic): Unit = {
    val properties = PropertySupport.extractPropertiesWithAccessors[Model[?]]
    properties.foreach { propWithAccessor =>
      if (!JsonHelpers.isIgnored(propWithAccessor.descriptor)) {
        val fieldName = propWithAccessor.descriptor.name
        val rawValue = json.selectDynamic(fieldName).asInstanceOf[js.Any]
        if (!js.isUndefined(rawValue) && rawValue != null) {
          val decoded = deserializeValue(rawValue, propWithAccessor.descriptor.propertyType)
          try {
            assignValue(model, propWithAccessor.accessor.asInstanceOf[PropertyAccessor[Any, Any]], decoded)
          } catch {
            case _: UnsupportedOperationException => // Skip read-only properties
          }
        }
      }
    }
  }

  private def deserializeValue(rawValue: js.Any, propType: TypeDescriptor): Any = {
    propType match {
      case pt: reflect.ParameterizedTypeDescriptor if isMapType(pt) =>
        deserializeMap(rawValue.asInstanceOf[Dynamic], pt)
      case cd: reflect.ClassDescriptor if isMapType(cd) =>
        deserializeMap(rawValue.asInstanceOf[Dynamic], cd)
      case _ =>
        val deserializer = DeserializerFactory.buildFromType(propType)
        deserializer.deserialize(rawValue.asInstanceOf[Dynamic], new JsonContext(propType))
    }
  }

  private def isMapType(tpe: TypeDescriptor): Boolean = tpe match {
    case pt: reflect.ParameterizedTypeDescriptor => pt.rawType.typeName == "scala.collection.immutable.Map" || pt.rawType.typeName == "Map"
    case cd: reflect.ClassDescriptor => cd.typeName == "scala.collection.immutable.Map" || cd.typeName == "Map"
    case _ => false
  }

  private def deserializeMap(json: Dynamic, mapType: TypeDescriptor): Map[String, Any] = {
    val elemType = mapType match {
      case pt: reflect.ParameterizedTypeDescriptor if pt.typeArguments.length >= 2 => pt.typeArguments(1)
      case _ => throw new IllegalStateException("Map must have two type arguments")
    }
    val jsonObj = json.asInstanceOf[js.Dynamic]
    val keys = js.Dynamic.global.Object.keys(jsonObj).asInstanceOf[js.Array[String]]
    val builder = Map.newBuilder[String, Any]
    var i = 0
    while (i < keys.length) {
      val key = keys(i)
      if (key != "@type" && !key.startsWith("$")) {
        val elemJson = jsonObj.selectDynamic(key).asInstanceOf[Dynamic]
        val deserializer = DeserializerFactory.buildFromType(elemType)
        val value = deserializer.deserialize(elemJson, new JsonContext(elemType))
        builder += ((key, value))
      }
      i += 1
    }
    builder.result()
  }

  private def assignValue(model: Model[?], property: PropertyAccessor[Any, Any], decoded: Any): Unit = {
    property.get(model) match {
      case p: Property[Any @unchecked] => p.set(decoded)
      case list: ListProperty[Any @unchecked] =>
        if (decoded != null) {
          list.clear()
          decoded match {
            case values: js.Array[?] => values.foreach(list.addOne)
            case values: Iterable[?] => values.foreach(list.addOne)
            case single => list.addOne(single)
          }
        }
      case _ => if (decoded != null) property.set(model, decoded)
    }
  }
}
