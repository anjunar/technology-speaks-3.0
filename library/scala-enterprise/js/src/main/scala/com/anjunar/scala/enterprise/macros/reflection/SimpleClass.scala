package com.anjunar.scala.enterprise.macros.reflection

class SimpleClass[T](val typeName: String) extends Type {
  def runtimeClass: Class[T] = null.asInstanceOf[Class[T]]
  override def getTypeName: String = typeName
}
