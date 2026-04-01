package jfx.json

import com.anjunar.scala.enterprise.macros.{Annotation, MetaClassLoader, PropertyAccess}
import com.anjunar.scala.enterprise.macros.reflection.SimpleClass

object JsonHelpers {

  def getJsonFieldName(access: PropertyAccess[?, ?]): String =
    access.annotations
      .collectFirst {
        case Annotation(className, params) if className == "com.anjunar.scala.enterprise.macros.validation.JsonName" =>
          params.getOrElse("value", access.name).asInstanceOf[String]
      }
      .getOrElse(access.name)

  def isIgnored(access: PropertyAccess[?, ?]): Boolean = {
    val name = access.name
    name == "meta" || name == "##" || name.startsWith("$") || access.annotations.exists {
      case Annotation(className, _) => className == "jfx.json.JsonIgnore"
      case null => false
    }
  }

  def findFactory(jsonType: Option[String], expectedType: SimpleClass[?]): () => Any = {
    if (expectedType.typeName == "scala.scalajs.js.Array") return () => null
    
    jsonType match {
      case Some(typeName) =>
        MetaClassLoader.getByTypeName(typeName).flatMap(MetaClassLoader.factories.get)
          .orElse(MetaClassLoader.factories.collectFirst { case (k, f) if k.typeName == typeName => f })
          .orElse {
            val simpleName = typeName.split('.').last
            MetaClassLoader.factories.collectFirst { case (k, f) if k.typeName.split('.').last == simpleName => f }
          }
          .getOrElse(throw IllegalArgumentException(s"No factory for '$typeName'"))
      case None =>
        MetaClassLoader.factories.get(expectedType)
          .orElse(expectedType.subTypes.collectFirst { case st if MetaClassLoader.factories.contains(st) => MetaClassLoader.factories(st) })
          .getOrElse(throw IllegalArgumentException(s"No factory for '${expectedType.typeName}'"))
    }
  }
}
