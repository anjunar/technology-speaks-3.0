package com.anjunar.scala.universe.introspector

import com.anjunar.scala.universe.{ResolvedClass, TypeResolver}

import java.lang.annotation.Annotation
import java.lang.reflect.Type
import java.util
import java.util.Objects

object AnnotationIntrospector {

  private val cache = new util.HashMap[(ResolvedClass, Class[? <: Annotation]), AnnotationModel]

  def create(aClass: ResolvedClass, annotationClass: Class[? <: Annotation]): AnnotationModel = {
    val key = (aClass, annotationClass)
    var model = cache.get(key)
    if (Objects.isNull(model)) {
      model = new AnnotationModel(aClass, annotationClass)
      cache.put(key, model)
    }
    model
  }

  def createWithType(aType: Type, annotationClass: Class[? <: Annotation]): AnnotationModel =
    create(TypeResolver.resolve(aType), annotationClass)

}
