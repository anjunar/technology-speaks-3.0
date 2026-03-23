package com.anjunar.scala.universe.introspector

import com.anjunar.scala.universe.members.{ResolvedField, ResolvedMethod}

class AnnotationProperty(val owner: AnnotationModel,
                         name: String,
                         field: ResolvedField,
                         getter: ResolvedMethod,
                         setter: ResolvedMethod) extends AbstractProperty(name, field, getter, setter) {
}
