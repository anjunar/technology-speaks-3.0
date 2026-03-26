package com.anjunar.json.mapper.schema

import com.anjunar.json.mapper.schema.property.Property
import com.anjunar.scala.universe.introspector.AbstractProperty

class DefaultWritableRule[E] extends VisibilityRule[E] {

  override def isVisible(instance: E, property: Property[E, Any]): Boolean = true

  override def isWriteable(instance: E, property: Property[E, Any]): Boolean = true

}
