package com.anjunar.scala.universe.introspector

import com.anjunar.scala.universe.members.{ResolvedField, ResolvedMethod}

class BeanProperty(val owner: BeanModel,
                   name: String,
                   field: ResolvedField,
                   getter: ResolvedMethod,
                   setter: ResolvedMethod) extends AbstractProperty(name, field, getter, setter) {
}
