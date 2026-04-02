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
    false
  }

  def findFactory(jsonType: Option[String], expectedType: ClassDescriptor): () => Any = {
    import reflect.ReflectRegistry
    
    if (expectedType.typeName == "scala.scalajs.js.Array") return () => null

    jsonType match {
      case Some(typeName) =>
        ReflectRegistry.loadClass(typeName)
          .flatMap(desc => ReflectRegistry.createInstance(desc.typeName))
          .orElse(ReflectRegistry.loadClassBySimpleName(typeName.split('.').last)
            .flatMap(desc => ReflectRegistry.createInstance(desc.typeName)))
          .map(instance => () => instance)
          .getOrElse(throw IllegalArgumentException(s"No factory for '$typeName'"))
      case None =>
        expectedType.baseTypes
          .map(reflect.ReflectionSupport.resolveClass)
          .find(desc => ReflectRegistry.contains(desc.typeName))
          .map(desc => ReflectRegistry.createInstance(desc.typeName).get)
          .map(instance => () => instance)
          .getOrElse(throw IllegalArgumentException(s"No factory for '${expectedType.typeName}'"))
    }
  }
}
