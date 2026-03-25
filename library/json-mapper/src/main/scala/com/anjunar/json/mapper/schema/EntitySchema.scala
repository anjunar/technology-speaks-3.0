package com.anjunar.json.mapper.schema

import com.anjunar.json.mapper.schema.PropertyMacros.makeProperty

import scala.beans.BeanProperty

abstract class EntitySchema[T] {

  val properties: java.util.LinkedHashMap[String, Property[T, Any]] =
    new java.util.LinkedHashMap[String, Property[T, Any]]()

  protected inline def property[V](inline selector: T => V,
                                   rule: VisibilityRule[T] = new DefaultRule().asInstanceOf[VisibilityRule[T]]): Property[T, V] = {
    val property = PropertyMacros.makeProperty[T, V](selector, rule)
    properties.put(property.name, property.asInstanceOf[Property[T, Any]])
    property
  }
}
