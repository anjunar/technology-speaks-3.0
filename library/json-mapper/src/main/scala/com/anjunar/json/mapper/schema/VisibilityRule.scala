package com.anjunar.json.mapper.schema

import com.anjunar.json.mapper.schema.property.Property
import com.anjunar.scala.universe.introspector.AbstractProperty

trait VisibilityRule[E] {

  def isVisible(instance: E, property: Property[E, Any]): Boolean

  def isWriteable(instance: E, property: Property[E, Any]): Boolean

}
