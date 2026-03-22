package com.anjunar.json.mapper.schema

import scala.beans.BeanProperty

abstract class EntitySchema[T] {

  @BeanProperty val properties: java.util.LinkedHashMap[String, Property[T, Any]] =
    new java.util.LinkedHashMap[String, Property[T, Any]]()

  def property[V](
    name: String,
    propertyType: Class[?],
    visibilityRule: VisibilityRule[T] = new DefaultRule().asInstanceOf[VisibilityRule[T]],
    collectionType: Class[?] = null
  ): Property[T, V] = {
    val prop = new Property[T, V](propertyType, collectionType, visibilityRule)
    properties.put(name, prop.asInstanceOf[Property[T, Any]])
    prop
  }

}
