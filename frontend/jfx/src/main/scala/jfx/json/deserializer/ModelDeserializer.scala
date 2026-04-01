package jfx.json.deserializer

import com.anjunar.scala.enterprise.macros.{Annotation, MetaClassLoader, PropertyAccess}
import com.anjunar.scala.enterprise.macros.reflection.{ParameterizedType, SimpleClass, Type}
import jfx.core.state.{ListProperty, Property}
import jfx.form.Model
import jfx.json.JsonHelpers

import scala.scalajs.js
import scala.scalajs.js.Dynamic

class ModelDeserializer extends Deserializer[Model[?]] {

  override def deserialize(json: Dynamic, context: JsonContext): Any = {
    val modelType = context.resolvedType match {
      case sc: SimpleClass[?] => sc
      case pt: ParameterizedType => pt.rawType match {
        case sc: SimpleClass[?] => sc
        case _ => throw new IllegalArgumentException(s"Expected SimpleClass, got ${context.resolvedType}")
      }
      case _ => throw new IllegalArgumentException(s"Expected SimpleClass or ParameterizedType, got ${context.resolvedType}")
    }

    modelType.typeName match {
      case "scala.scalajs.js.Array" =>
        val deserializer = new JsArrayDeserializer()
        return deserializer.deserialize(json, context)
      case "scala.collection.immutable.Map" | "Map" =>
        context.resolvedType match {
          case pt: ParameterizedType => return deserializeMap(json, pt)
          case _ => throw new IllegalArgumentException("Map must be parameterized")
        }
      case _ =>
    }

    val factory = JsonHelpers.findFactory(readJsonType(json), modelType)
    val model = factory().asInstanceOf[Model[?]]
    val typeResolver = context.resolvedType match {
      case pt: ParameterizedType => createTypeResolver(pt)
      case _ => identity[Type]
    }
    populateModel(model, json, typeResolver)
    model
  }

  private def createTypeResolver(pt: ParameterizedType): Type => Type = {
    val typeArgs = pt.typeArguments
    val typeParamIndex = pt.rawType match {
      case sc: SimpleClass[?] => sc.typeParameters.zipWithIndex.toMap
      case _ => Map.empty[String, Int]
    }
    (tpe: Type) => tpe match {
      case pt2: ParameterizedType =>
        com.anjunar.scala.enterprise.macros.ReflectionSupport.parameterized(
          pt2.rawType.asInstanceOf[SimpleClass[?]],
          pt2.typeArguments.map(resolveTypeArgument(_, typeArgs, typeParamIndex))
        )
      case _ => resolveTypeArgument(tpe, typeArgs, typeParamIndex)
    }
  }

  private def resolveTypeArgument(tpe: Type, typeArgs: Array[Type], typeParamIndex: Map[String, Int]): Type =
    typeParamIndex.get(tpe.getTypeName) match {
      case Some(i) if i < typeArgs.length => typeArgs(i)
      case _ => tpe
    }

  private def readJsonType(json: Dynamic): Option[String] =
    Option(json.selectDynamic("@type").asInstanceOf[js.Any])
      .filter(v => v != null && !js.isUndefined(v))
      .map(_.toString)

  private def populateModel(model: Model[?], json: Dynamic, typeResolver: Type => Type): Unit = {
    model.meta.properties.foreach { property =>
      if (!JsonHelpers.isIgnored(property)) {
        val resolvedType = typeResolver(property.genericType)
        val fieldName = JsonHelpers.getJsonFieldName(property)
        val decoded = resolvedType match {
          case pt: ParameterizedType if isMapType(pt) => deserializeMap(json, pt)
          case sc: SimpleClass[?] if isMapType(sc) => deserializeMap(json, sc)
          case _ =>
            val rawValue = json.selectDynamic(fieldName).asInstanceOf[js.Any]
            if (js.isUndefined(rawValue) || rawValue == null) null
            else {
              val deserializer = DeserializerFactory.buildFromType(resolvedType)
              deserializer.deserialize(rawValue.asInstanceOf[Dynamic], new JsonContext(resolvedType))
            }
        }
        try {
          assignValue(model, property.asInstanceOf[PropertyAccess[Any, Any]], decoded)
        } catch {
          case _: UnsupportedOperationException => // Skip read-only properties
        }
      }
    }
  }

  private def isMapType(tpe: Type): Boolean = tpe match {
    case pt: ParameterizedType => pt.rawType match {
      case sc: SimpleClass[?] => sc.typeName == "scala.collection.immutable.Map" || sc.typeName == "Map"
      case _ => false
    }
    case sc: SimpleClass[?] => sc.typeName == "scala.collection.immutable.Map" || sc.typeName == "Map"
    case _ => false
  }

  private def deserializeMap(json: Dynamic, mapType: Type): Map[String, Any] = {
    val elemType = mapType match {
      case pt: ParameterizedType if pt.typeArguments.length >= 2 => pt.typeArguments(1)
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

  private def assignValue(model: Model[?], property: PropertyAccess[Any, Any], decoded: Any): Unit = {
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
