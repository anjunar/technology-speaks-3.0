package com.anjunar.json.mapper.macros

import java.lang.annotation.Annotation
import java.lang.reflect.{Field, GenericArrayType, ParameterizedType, Type}

object RuntimeTypeResolver {

  def resolveClass(name: String): Class[?] =
    name match {
      case "scala.Int" => java.lang.Integer.TYPE
      case "scala.Long" => java.lang.Long.TYPE
      case "scala.Double" => java.lang.Double.TYPE
      case "scala.Float" => java.lang.Float.TYPE
      case "scala.Boolean" => java.lang.Boolean.TYPE
      case "scala.Byte" => java.lang.Byte.TYPE
      case "scala.Short" => java.lang.Short.TYPE
      case "scala.Char" => java.lang.Character.TYPE
      case "scala.Unit" => java.lang.Void.TYPE
      case other => Class.forName(other)
    }

  def parameterized(raw: Class[?], args: Array[Type]): ParameterizedType =
    SimpleParameterizedType(raw, args.clone())

  def genericArray(component: Type): GenericArrayType =
    SimpleGenericArrayType(component)

  def typeVariable(name: String): Type =
    SimpleTypeVariable(name)

  private final case class SimpleParameterizedType(
                                                    raw: Class[?],
                                                    args: Array[Type]
                                                  ) extends ParameterizedType {
    override def getRawType: Type = raw

    override def getActualTypeArguments: Array[Type] = args

    override def getOwnerType: Type = null
  }

  private final case class SimpleGenericArrayType(
                                                   component: Type
                                                 ) extends GenericArrayType {
    override def getGenericComponentType: Type = component
  }

  private final case class SimpleTypeVariable(
                                               name: String
                                             ) extends Type {
    override def getTypeName: String = name

    override def toString: String = name
  }    
}