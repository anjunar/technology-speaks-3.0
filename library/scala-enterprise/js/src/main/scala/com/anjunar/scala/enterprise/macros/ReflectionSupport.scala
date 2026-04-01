package com.anjunar.scala.enterprise.macros

import com.anjunar.scala.enterprise.macros.reflection.{GenericArrayType, ParameterizedType, SimpleClass, Type}

object ReflectionSupport {

  def resolveClass(name: String): SimpleClass[?] = {
    new SimpleClass(name)
  }

  def resolveClassWithParams(name: String, typeParams: Array[String]): SimpleClass[?] = {
    new SimpleClass(name, typeParameters = typeParams)
  }

  def resolveClassWithAnnotations(name: String, annotations: Array[Annotation]): SimpleClass[?] = {
    new SimpleClass(name, annotations)
  }

  def resolveClassWithProperties[T](name: String, properties: Array[PropertyAccess[T, ?]]): SimpleClass[T] = {
    new SimpleClass(name, properties = properties)
  }

  def resolveClassFull[T](name: String, annotations: Array[Annotation], properties: Array[PropertyAccess[T, ?]]): SimpleClass[T] = {
    new SimpleClass(name, annotations, properties)
  }

  def parameterized(raw: SimpleClass[?], args: Array[Type]): ParameterizedType =
    SimpleParameterizedType(raw, args.clone())

  def genericArray(component: Type): GenericArrayType =
    SimpleGenericArrayType(component)

  def typeVariable(name: String): Type =
    SimpleTypeVariable(name)

  private final case class SimpleParameterizedType(raw: SimpleClass[?],
                                                   args: Array[Type]) extends ParameterizedType {

    override def typeArguments: Array[Type] = args

    override def rawType: Type = raw

    override def getTypeName: String = rawType.getTypeName + args.mkString("[", ", ", "]")
  }

  private final case class SimpleGenericArrayType(component: Type) extends GenericArrayType {
    override def getGenericComponentType: Type = component

    override def getTypeName: String = s"${component.getTypeName}[]"
  }

  private final case class SimpleTypeVariable(name: String) extends Type {
    override def getTypeName: String = name

    override def toString: String = name
  }
}
