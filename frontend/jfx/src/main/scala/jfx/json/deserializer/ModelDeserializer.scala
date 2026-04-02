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

    // Create instance directly from registry using type name
    val model = reflect.ReflectRegistry.createInstance(modelType.typeName) match {
      case Some(m) => m.asInstanceOf[Model[?]]
      case None => throw new IllegalArgumentException(s"Cannot create instance for ${modelType.typeName}")
    }
    populateModel(model, json, context.resolvedType)
    model
  }

  private def readJsonType(json: Dynamic): Option[String] =
    Option(json.selectDynamic("@type").asInstanceOf[js.Any])
      .filter(v => v != null && !js.isUndefined(v))
      .map(_.toString)

  private def populateModel(model: Model[?], json: Dynamic, typeDescriptor: reflect.TypeDescriptor): Unit = {
    // Get properties from registry instead of typeDescriptor to avoid recursion issues
    val typeName = typeDescriptor.typeName
    val classDescriptor = reflect.ReflectRegistry.loadClass(typeName).getOrElse(
      throw new IllegalArgumentException(s"Cannot find class descriptor for $typeName")
    )
    val properties = classDescriptor.properties
    
    properties.foreach { prop =>
      if (!JsonHelpers.isIgnored(prop)) {
        val fieldName = prop.name
        val rawValue = json.selectDynamic(fieldName).asInstanceOf[js.Any]
        if (!js.isUndefined(rawValue) && rawValue != null) {
          val decoded = deserializeValue(rawValue, prop.propertyType)
          assignValue(model, typeName, prop, decoded)
        }
      }
    }
  }

  private def deserializeValue(rawValue: js.Any, propType: TypeDescriptor): Any = {
    propType match {
      case pt: reflect.ParameterizedTypeDescriptor if isOptionType(pt) && pt.typeArguments.nonEmpty =>
        // Handle Option[T] directly - don't try to create Option instance
        if (rawValue == null || js.isUndefined(rawValue)) {
          None
        } else {
          val elementType = pt.typeArguments(0)
          val deserializer = DeserializerFactory.buildFromType(elementType)
          val value = deserializer.deserialize(rawValue.asInstanceOf[Dynamic], new JsonContext(elementType))
          Some(value)
        }
      case pt: reflect.ParameterizedTypeDescriptor if isListPropertyType(pt) && pt.typeArguments.nonEmpty =>
        // Handle ListProperty[T] - use ListPropertyDeserializer
        val deserializer = DeserializerFactory.buildFromType(pt)
        deserializer.deserialize(rawValue.asInstanceOf[Dynamic], new JsonContext(pt))
      case pt: reflect.ParameterizedTypeDescriptor if isPropertyType(pt) && pt.typeArguments.nonEmpty =>
        // Handle Property[T] - unwrap and deserialize element type
        val elementType = pt.typeArguments(0)
        val deserializer = DeserializerFactory.buildFromType(elementType)
        deserializer.deserialize(rawValue.asInstanceOf[Dynamic], new JsonContext(elementType))
      case pt: reflect.ParameterizedTypeDescriptor if isMapType(pt) =>
        deserializeMap(rawValue.asInstanceOf[Dynamic], pt)
      case cd: reflect.ClassDescriptor if isMapType(cd) =>
        deserializeMap(rawValue.asInstanceOf[Dynamic], cd)
      case _ =>
        val deserializer = DeserializerFactory.buildFromType(propType)
        deserializer.deserialize(rawValue.asInstanceOf[Dynamic], new JsonContext(propType))
    }
  }

  private def isPropertyType(tpe: TypeDescriptor): Boolean = {
    tpe.typeName == "jfx.core.state.Property" || tpe.typeName == "Property"
  }
  
  private def isListPropertyType(tpe: TypeDescriptor): Boolean = {
    tpe.typeName == "jfx.core.state.ListProperty" || tpe.typeName == "ListProperty"
  }
  
  private def isOptionType(tpe: TypeDescriptor): Boolean = {
    tpe.typeName == "scala.Option" || tpe.typeName == "Option"
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

  private def assignValue(model: Model[?], typeName: String, prop: reflect.PropertyDescriptor, decoded: Any): Unit = {
    reflect.ReflectRegistry.getPropertyAccessor(typeName, prop.name) match {
      case Some(accessor) =>
        accessor.get(model) match {
          case p: Property[Any @unchecked] =>
            // Handle Option values - if decoded is None and property is Option, set directly
            p.set(decoded)
          case list: ListProperty[Any @unchecked] =>
            // decoded is either a ListProperty or js.Array from ListPropertyDeserializer
            decoded match {
              case newList: ListProperty[Any @unchecked] =>
                // Replace all elements
                list.clear()
                newList.underlying.foreach(list.addOne)
              case arr: js.Array[?] =>
                list.clear()
                arr.foreach(list.addOne)
              case values: Iterable[?] =>
                list.clear()
                values.foreach(list.addOne)
              case single =>
                list.clear()
                list.addOne(single)
            }
          case _ => if (decoded != null) accessor.set(model, decoded)
        }
      case None =>
        js.Dynamic.global.console.error(s"No accessor found for $typeName.${prop.name}")
    }
  }
}
