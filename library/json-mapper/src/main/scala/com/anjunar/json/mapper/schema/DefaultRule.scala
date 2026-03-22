package com.anjunar.json.mapper.schema

import com.anjunar.scala.universe.introspector.AbstractProperty

class DefaultRule extends VisibilityRule[Any] {

  override def isVisible(instance: Any, property: AbstractProperty): Boolean = true

  override def isWriteable(instance: Any, property: AbstractProperty): Boolean = false

}
