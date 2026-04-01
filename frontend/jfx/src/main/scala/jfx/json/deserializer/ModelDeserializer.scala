package jfx.json.deserializer

import com.anjunar.scala.enterprise.macros.{Annotation, MetaClassLoader, PropertyAccess}
import com.anjunar.scala.enterprise.macros.reflection.{ParameterizedType, SimpleClass}
import jfx.core.state.{ListProperty, Property}
import jfx.form.Model

import scala.scalajs.js
import scala.scalajs.js.Dynamic

class ModelDeserializer extends Deserializer[Model[?]] {

  override def deserialize(json: Dynamic, context: JsonContext): Any = {
    val modelType = context.resolvedType match {
      case sc: SimpleClass[?] => sc
      case pt: ParameterizedType =>
        pt.rawType match {
          case sc: SimpleClass[?] => sc
          case _ => throw new IllegalArgumentException(s"Expected SimpleClass, got ${context.resolvedType}")
        }
      case _ => throw new IllegalArgumentException(s"Expected SimpleClass or ParameterizedType, got ${context.resolvedType}")
    }

    val jsonType = readJsonType(json)
    val factory = findFactory(jsonType, modelType)

    val model = factory().asInstanceOf[Model[?]]
    populateModel(model, json, context)
    model
  }

  private def readJsonType(json: Dynamic): Option[String] = {
    val rawType = json.selectDynamic("@type").asInstanceOf[js.Any]
    if (rawType == null || js.isUndefined(rawType)) None
    else Some(rawType.toString)
  }

  private def findFactory(jsonType: Option[String], expectedType: SimpleClass[?]): () => Any = {
    jsonType match {
      case Some(typeName) =>
        MetaClassLoader.getByTypeName(typeName)
          .flatMap(MetaClassLoader.factories.get) match {
          case Some(factory) => factory
          case None =>
            MetaClassLoader.factories.collectFirst {
              case (key, factory) if key.typeName == typeName => factory
            }.getOrElse {
              val simpleTypeName = typeName.split('.').last
              MetaClassLoader.factories.collectFirst {
                case (key, factory) if key.typeName.split('.').last == simpleTypeName => factory
              }.getOrElse {
                throw IllegalArgumentException(s"No factory registered for type '$typeName'. Available types: ${MetaClassLoader.factories.keys.map(_.typeName).mkString(", ")}")
              }
            }
        }
      case None =>
        MetaClassLoader.factories.get(expectedType) match {
          case Some(factory) => factory
          case None =>
            val subTypeFactory = findSubTypeFactory(expectedType)
            subTypeFactory.getOrElse {
              throw IllegalArgumentException(s"No factory registered for type '${expectedType.typeName}'. Available types: ${MetaClassLoader.factories.keys.map(_.typeName).mkString(", ")}")
            }
        }
    }
  }

  private def findSubTypeFactory(expectedType: SimpleClass[?]): Option[() => Any] = {
    val subTypes = expectedType.subTypes
    subTypes.collectFirst {
      case subType if MetaClassLoader.factories.contains(subType) =>
        MetaClassLoader.factories(subType)
    }
  }

  private def populateModel(model: Model[?], json: Dynamic, parentContext: JsonContext): Unit = {
    model.meta.properties.foreach { property =>
      if (!isIgnored(property)) {
        val fieldName = getJsonFieldName(property)
        val rawValue = json.selectDynamic(fieldName).asInstanceOf[js.Any]

        if (!js.isUndefined(rawValue)) {
          if (rawValue == null) {
            assignValue(model, property.asInstanceOf[PropertyAccess[Any, Any]], null)
          } else {
            val deserializer = DeserializerFactory.buildFromType(property.genericType)
            val elemContext = new JsonContext(property.genericType)
            val decoded = deserializer.deserialize(rawValue.asInstanceOf[Dynamic], elemContext)
            assignValue(model, property.asInstanceOf[PropertyAccess[Any, Any]], decoded)
          }
        }
      }
    }
  }

  private def getJsonFieldName(access: PropertyAccess[?, ?]): String = {
    access.annotations
      .collectFirst {
        case Annotation(className, params) if className == "com.anjunar.scala.enterprise.macros.validation.JsonName" =>
          params.getOrElse("value", access.name).asInstanceOf[String]
      }
      .getOrElse(access.name)
  }

  private def isIgnored(access: PropertyAccess[?, ?]): Boolean = {
    val name = access.name
    name == "meta" || name == "##" || name.startsWith("$") || access.annotations.exists {
      case Annotation(className, _) => className == "jfx.json.JsonIgnore"
      case null => false
    }
  }

  private def assignValue(model: Model[?], property: PropertyAccess[Any, Any], decoded: Any): Unit = {
    property.get(model) match {
      case p: Property[Any @unchecked] =>
        p.set(decoded)
      case list: ListProperty[Any @unchecked] =>
        list.clear()
        decoded match {
          case values: js.Array[?] =>
            var i = 0
            while (i < values.length) {
              list.addOne(values(i))
              i += 1
            }
          case values: Iterable[?] =>
            values.foreach(v => list.addOne(v))
          case single =>
            list.addOne(single)
        }
      case _ => property.set(model, decoded)
    }
  }

}
