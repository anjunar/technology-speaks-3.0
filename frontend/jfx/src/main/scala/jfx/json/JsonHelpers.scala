package jfx.json

import reflect.macros.PropertySupport
import reflect.{ClassDescriptor, PropertyAccessor, PropertyDescriptor}

object JsonHelpers {

  def getJsonFieldName(prop: PropertyDescriptor): String =
    prop.annotations
      .collectFirst {
        case ann if ann.annotationClassName == "jfx.json.JsonName" =>
          ann.parameters.getOrElse("value", prop.name).asInstanceOf[String]
      }
      .getOrElse(prop.name)

  def isIgnored(prop: PropertyDescriptor): Boolean = {
    val name = prop.name
    name == "meta" || name == "##" || name.startsWith("$") || prop.annotations.exists { ann =>
      ann != null && ann.annotationClassName == "jfx.json.JsonIgnore"
    }
  }

  def isIgnored(accessor: PropertyAccessor[?, ?]): Boolean = {
    // Cannot check annotations on PropertyAccessor, need to use PropertyDescriptor
    false
  }

  def findFactory(jsonType: Option[String], expectedType: ClassDescriptor): () => Any = {
    if (expectedType.typeName == "scala.scalajs.js.Array") return () => null

    jsonType match {
      case Some(typeName) =>
        reflect.ReflectRegistry.factoriesByTypeName.get(typeName).flatMap(reflect.ReflectRegistry.factories.get)
          .orElse(reflect.ReflectRegistry.factories.collectFirst { case (k, f) if k.typeName == typeName => f })
          .orElse {
            val simpleName = typeName.split('.').last
            reflect.ReflectRegistry.factories.collectFirst { case (k, f) if k.typeName.split('.').last == simpleName => f }
          }
          .getOrElse(throw IllegalArgumentException(s"No factory for '$typeName'"))
      case None =>
        reflect.ReflectRegistry.factories.get(expectedType)
          .orElse(expectedType.baseTypes.map(reflect.ReflectionSupport.resolveClass).collectFirst { case st if reflect.ReflectRegistry.factories.contains(st) => reflect.ReflectRegistry.factories(st) })
          .getOrElse(throw IllegalArgumentException(s"No factory for '${expectedType.typeName}'"))
    }
  }
}
