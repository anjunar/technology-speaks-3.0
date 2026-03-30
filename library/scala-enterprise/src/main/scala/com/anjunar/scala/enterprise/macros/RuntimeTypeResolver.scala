package com.anjunar.scala.enterprise.macros

import com.anjunar.scala.enterprise.macros.reflection.{GenericArrayType, ParameterizedType, SimpleClass, Type}

object RuntimeTypeResolver {

  def resolveClass(name: String): SimpleClass[?] = {

    val underlying = name match {
      case "scala.Int" => java.lang.Integer.TYPE
      case "scala.Long" => java.lang.Long.TYPE
      case "scala.Double" => java.lang.Double.TYPE
      case "scala.Float" => java.lang.Float.TYPE
      case "scala.Boolean" => java.lang.Boolean.TYPE
      case "scala.Byte" => java.lang.Byte.TYPE
      case "scala.Short" => java.lang.Short.TYPE
      case "scala.Char" => java.lang.Character.TYPE
      case "scala.Unit" => java.lang.Void.TYPE
      case "scala.Any" | "scala.AnyRef" => classOf[java.lang.Object]
      case other => Class.forName(other)
    }

    new SimpleClass(underlying)
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