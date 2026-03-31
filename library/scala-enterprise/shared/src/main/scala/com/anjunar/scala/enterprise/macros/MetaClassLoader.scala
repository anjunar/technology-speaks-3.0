package com.anjunar.scala.enterprise.macros

import com.anjunar.scala.enterprise.macros.reflection.SimpleClass

import scala.collection.mutable

object MetaClassLoader {
  
  val factories : mutable.Map[SimpleClass[?], () => Any] = mutable.Map.empty
  val classes : mutable.Map[Class[?], SimpleClass[?]] = mutable.Map.empty
  
  def register(clazz: SimpleClass[?], instance: () => Any): Unit = {
    factories += clazz -> instance
    classes += clazz.runtimeClass -> clazz
  }

}
