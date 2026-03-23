package com.anjunar.scala.universe.introspector

import com.anjunar.scala.universe.ResolvedClass
import com.anjunar.scala.universe.annotations.Annotated
import com.anjunar.scala.universe.members.{ResolvedField, ResolvedMethod}

import java.lang.annotation.Annotation

class AnnotationModel(val underlying: ResolvedClass, val annotationClass: Class[? <: Annotation]) extends Annotated {

  private def getPropertyName(name: String): String = {
    if (name.endsWith("_$eq")) {
      name.substring(0, name.length - 4)
    } else {
      name
    }
  }

  private def isAnnotated(annotated: Annotated): Boolean = {
    annotated.annotations.exists(_.annotationType() == annotationClass)
  }

  private def collectProperties(fields: Array[ResolvedField], methods: Array[ResolvedMethod]): Array[AnnotationProperty] = {
    val propertyNames = (
      fields.filter(isAnnotated).map(_.name) ++
      methods.filter(isAnnotated).map(m => getPropertyName(m.name))
    ).distinct

    propertyNames.map { name =>
      val field = fields.find(_.name == name).orNull
      val getter = methods.find(m => m.name == name && m.parameters.length == 0).orNull
      val setter = methods.find(m => m.name == (name + "_$eq") && m.parameters.length == 1).orNull
      new AnnotationProperty(this, name, field, getter, setter)
    }
  }

  lazy val declaredProperties: Array[AnnotationProperty] = collectProperties(underlying.declaredFields, underlying.declaredMethods)

  lazy val properties: Array[AnnotationProperty] = collectProperties(underlying.fields, underlying.methods)

  def findDeclaredProperty(name: String): AnnotationProperty = declaredProperties.find(_.name == name).orNull

  def findProperty(name: String): AnnotationProperty = properties.find(_.name == name).orNull

  override lazy val declaredAnnotations: Array[Annotation] = underlying.declaredAnnotations
  override lazy val annotations: Array[Annotation] = underlying.annotations
}
