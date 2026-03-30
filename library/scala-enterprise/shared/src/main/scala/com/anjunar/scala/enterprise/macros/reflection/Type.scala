package com.anjunar.scala.enterprise.macros.reflection

trait Type {
  def getTypeName: String
}

trait ParameterizedType extends Type {
  def typeArguments: Array[Type]
  def rawType: Type
}

trait GenericArrayType extends Type {
  def getGenericComponentType: Type
}
