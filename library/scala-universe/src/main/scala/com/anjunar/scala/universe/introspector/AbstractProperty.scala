package com.anjunar.scala.universe.introspector

import com.anjunar.scala.universe.annotations.Annotated
import com.anjunar.scala.universe.members.{ResolvedField, ResolvedMethod}
import com.anjunar.scala.universe.{ResolvedClass, TypeResolver}
import com.google.common.reflect.TypeToken

import java.lang.annotation.Annotation

class AbstractProperty(val name : String,
                       val field : ResolvedField,
                       val getter : ResolvedMethod,
                       val setter : ResolvedMethod) extends Annotated {

  def get(instance : AnyRef) : Any = {
    if (getter == null) {
      field.get(instance)
    } else {
      getter.invoke(instance)  
    }
  }

  def set(instance : AnyRef, value : Any) : Unit = {
    if (setter == null) {
      throw new IllegalStateException("no Setter Method " + toString)
    }
    setter.invoke(instance, value)
  }

  def propertyType : ResolvedClass = {
    if (getter == null) {
      TypeResolver.resolve(TypeToken.of(field.fieldType.underlying).wrap().getType)
    } else {
      TypeResolver.resolve(TypeToken.of(getter.returnType.underlying).wrap().getType)  
    }
  }

  val isWriteable : Boolean = setter != null

  override lazy val declaredAnnotations: Array[Annotation] = {
    val fieldAnnotation : Array[Annotation] = if (field == null) {
      Array()
    } else {
      field.declaredAnnotations
    }

    val setterAnnotation : Array[Annotation] = if (setter == null) {
      Array()
    } else {
      setter.declaredAnnotations
    }

    val getterAnnotation : Array[Annotation] = getter.declaredAnnotations

    fieldAnnotation ++ getterAnnotation ++ setterAnnotation
  }

  override lazy val annotations: Array[Annotation] = {
    val fieldAnnotation : Array[Annotation] = if (field == null) {
      Array()
    } else {
      field.annotations
    }

    val setterAnnotation : Array[Annotation] = if (setter == null) {
      Array()
    } else {
      setter.annotations
    }

    val getterAnnotation : Array[Annotation] = if (getter == null) {
      Array()
    } else {
      getter.annotations
    }

    fieldAnnotation ++ getterAnnotation ++ setterAnnotation
  }

  override def toString = (if isWriteable then s"write $name" else s"read $name") + s": $propertyType"


}
