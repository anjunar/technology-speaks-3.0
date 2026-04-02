package jfx.json.deserializer

import reflect.macros.PropertySupport
import reflect.{PropertyAccessor, TypeDescriptor}
import jfx.core.state.{ListProperty, Property}
import jfx.json.JsonHelpers

import scala.scalajs.js
import scala.scalajs.js.Dynamic

class ModelDeserializer extends Deserializer[AnyRef] {

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

    // Resolve polymorphic type using @type field in JSON
    val actualTypeName = modelType match {
      case cd: reflect.ClassDescriptor => resolvePolymorphicType(json, cd)
      case _ => modelType.typeName
    }

    // Create instance directly from registry using type name
    val model = reflect.ReflectRegistry.createInstance(actualTypeName) match {
      case Some(m) => m.asInstanceOf[AnyRef]
      case None => throw new IllegalArgumentException(s"Cannot create instance for $actualTypeName")
    }
    populateModel(model, json, actualTypeName)
    model
  }

  private def readJsonType(json: Dynamic): Option[String] =
    Option(json.selectDynamic("@type").asInstanceOf[js.Any])
      .filter(v => v != null && !js.isUndefined(v))
      .map(_.toString)

  private def resolvePolymorphicType(json: Dynamic, modelType: reflect.ClassDescriptor): String = {
    readJsonType(json) match {
      case Some(jsonTypeName) =>
        jfx.json.JsonTypeRegistry.resolveType(jsonTypeName) match {
          case Some(concreteDescriptor) => concreteDescriptor.typeName
          case None =>
            js.Dynamic.global.console.warn(s"Unknown JSON type: $jsonTypeName")
            modelType.typeName
        }
      case None => modelType.typeName
    }
  }

  private def populateModel(model: AnyRef, json: Dynamic, typeName: String): Unit = {
    // Get properties from registry instead of typeDescriptor to avoid recursion issues
    val classDescriptor = reflect.ReflectRegistry.loadClass(typeName).getOrElse(
      throw new IllegalArgumentException(s"Cannot find class descriptor for $typeName")
    )
    val properties = classDescriptor.properties

    properties.foreach { prop =>
      if (!JsonHelpers.isIgnored(prop)) {
        // Use JsonName annotation if present, otherwise use the property name
        val fieldName = prop.getAnnotation("jfx.json.JsonName") match {
          case Some(ann) => ann.parameters.getOrElse("value", prop.name).toString
          case None => prop.name
        }
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
      case pt: reflect.ParameterizedTypeDescriptor if isPropertyType(pt) && pt.typeArguments.nonEmpty =>
        // Handle Property[T] - use PropertyDeserializer which handles Option internally
        val deserializer = DeserializerFactory.buildFromType(pt)
        deserializer.deserialize(rawValue.asInstanceOf[Dynamic], new JsonContext(pt))
      case pt: reflect.ParameterizedTypeDescriptor if isListPropertyType(pt) && pt.typeArguments.nonEmpty =>
        // Handle ListProperty[T] - use ListPropertyDeserializer
        val deserializer = DeserializerFactory.buildFromType(pt)
        deserializer.deserialize(rawValue.asInstanceOf[Dynamic], new JsonContext(pt))
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

  private def assignValue(model: AnyRef, typeName: String, prop: reflect.PropertyDescriptor, decoded: Any): Unit = {
    reflect.ReflectRegistry.getPropertyAccessor(typeName, prop.name) match {
      case Some(accessor) =>
        accessor.get(model) match {
          case p: Property[Any @unchecked] =>
            // Set the value directly - decoded is already the correct type (including Option)
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
