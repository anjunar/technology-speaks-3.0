package com.anjunar.scala.enterprise.macros

import com.anjunar.scala.enterprise.macros.reflection.Type

import java.lang.annotation.Annotation

trait PropertyAccess[E, V] {

  val isWriteable: Boolean = false
  
  val name: String

  val annotations : Array[? <: Annotation]

  val genericType : Type

  def get(instance: E): V

  def set(instance: E, value: V): Unit

}
