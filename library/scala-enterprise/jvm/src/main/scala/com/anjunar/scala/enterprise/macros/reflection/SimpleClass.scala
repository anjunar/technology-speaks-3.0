package com.anjunar.scala.enterprise.macros.reflection

class SimpleClass[T](val runtimeClass: Class[T]) extends Type {
  override def getTypeName: String = runtimeClass.getName
}
