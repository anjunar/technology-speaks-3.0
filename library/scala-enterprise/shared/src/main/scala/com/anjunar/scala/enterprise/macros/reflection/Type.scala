package com.anjunar.scala.enterprise.macros.reflection

trait Type {
  def getTypeName: String
}

case class SimpleClass[T](typeName: String) extends Type {
  def runtimeClass: Class[T] = null.asInstanceOf[Class[T]]
  override def getTypeName: String = typeName
}

trait ParameterizedType extends Type {
  def typeArguments: Array[Type]
  def rawType: Type
  override def getTypeName: String = rawType.getTypeName
}

trait GenericArrayType extends Type {
  def getGenericComponentType: Type
  override def getTypeName: String = getGenericComponentType.getTypeName
}
