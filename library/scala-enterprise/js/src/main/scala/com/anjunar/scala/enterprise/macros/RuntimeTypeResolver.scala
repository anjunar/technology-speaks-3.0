package com.anjunar.scala.enterprise.macros

import com.anjunar.scala.enterprise.macros.reflection.{GenericArrayType, ParameterizedType, SimpleClass, Type}

object RuntimeTypeResolver {

  def resolveClass(name: String): SimpleClass[?] = {
    val underlying = name match {
      case "scala.Int" => classOf[Int]
      case "scala.Long" => classOf[Long]
      case "scala.Double" => classOf[Double]
      case "scala.Float" => classOf[Float]
      case "scala.Boolean" => classOf[Boolean]
      case "scala.Byte" => classOf[Byte]
      case "scala.Short" => classOf[Short]
      case "scala.Char" => classOf[Char]
      case "scala.Unit" => classOf[Unit]
      case "scala.Any" | "scala.AnyRef" => classOf[Any]
      case "java.util.UUID" => classOf[java.util.UUID]
      case "java.lang.String" => classOf[String]
      case other =>
        classOf[Any]
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
