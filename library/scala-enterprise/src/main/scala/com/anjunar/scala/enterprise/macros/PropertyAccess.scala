package com.anjunar.scala.enterprise.macros

import java.lang.annotation.Annotation
import java.lang.reflect.Type

trait PropertyAccess[E, V] {

  val isWriteable: Boolean = false
  
  val name: String

  val annotations : Array[? <: Annotation]

  val genericType : Type

  def get(instance: E): V

  def set(instance: E, value: V): Unit

}
