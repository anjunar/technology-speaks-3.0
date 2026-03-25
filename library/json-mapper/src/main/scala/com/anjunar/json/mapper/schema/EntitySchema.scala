package com.anjunar.json.mapper.schema


import com.anjunar.json.mapper.macros.PropertyMacros
import com.anjunar.scala.universe.TypeResolver

abstract class EntitySchema[T] {

  val properties: java.util.LinkedHashMap[String, Property[T, Any]] =
    new java.util.LinkedHashMap[String, Property[T, Any]]()

  protected inline def property[V](inline selector: T => V,
                                   rule: VisibilityRule[T] = new DefaultRule().asInstanceOf[VisibilityRule[T]]): Property[T, V] = {
    val property = PropertyMacros.makePropertyAccess[T, V](selector)
    val resolvedClass = TypeResolver.resolve(property.genericType)
    val headOption = resolvedClass.typeArguments.headOption
    val value = new Property[T, V](property.name, resolvedClass.raw, if headOption.isDefined then headOption.get.raw else null, rule)
    properties.put(property.name, value.asInstanceOf[Property[T, Any]])
    value
  }
}
