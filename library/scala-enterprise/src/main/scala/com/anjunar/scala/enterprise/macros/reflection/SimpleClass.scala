package com.anjunar.scala.enterprise.macros.reflection

class SimpleClass[E](val underlying : Class[E]) extends Type {
  
  override def getTypeName: String = underlying.getName
  
}
