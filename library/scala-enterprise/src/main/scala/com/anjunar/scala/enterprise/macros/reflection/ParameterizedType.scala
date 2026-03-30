package com.anjunar.scala.enterprise.macros.reflection

trait ParameterizedType extends Type {
  
  def typeArguments: Array[Type]
  
  def rawType: Type

}
