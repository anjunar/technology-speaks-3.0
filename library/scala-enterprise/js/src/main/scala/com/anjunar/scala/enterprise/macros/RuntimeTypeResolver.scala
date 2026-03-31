package com.anjunar.scala.enterprise.macros

import com.anjunar.scala.enterprise.macros.reflection.{GenericArrayType, ParameterizedType, SimpleClass, Type}

object RuntimeTypeResolver {

  def resolveClass(name: String): SimpleClass[?] =
    ReflectionSupport.resolveClass(name)

  def resolveClassWithAnnotations(name: String, annotations: Array[Annotation]): SimpleClass[?] =
    ReflectionSupport.resolveClassWithAnnotations(name, annotations)

  def resolveClassWithProperties[T](name: String, properties: Array[PropertyAccess[T, ?]]): SimpleClass[T] =
    ReflectionSupport.resolveClassWithProperties(name, properties)

  def resolveClassFull[T](name: String, annotations: Array[Annotation], properties: Array[PropertyAccess[T, ?]]): SimpleClass[T] =
    ReflectionSupport.resolveClassFull(name, annotations, properties)

  def parameterized(raw: SimpleClass[?], args: Array[Type]): ParameterizedType =
    ReflectionSupport.parameterized(raw, args)

  def genericArray(component: Type): GenericArrayType =
    ReflectionSupport.genericArray(component)

  def typeVariable(name: String): Type =
    ReflectionSupport.typeVariable(name)
}
