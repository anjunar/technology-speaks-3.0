package com.anjunar.scala.enterprise.macros

import com.anjunar.scala.enterprise.macros.reflection.SimpleClass

import scala.collection.mutable

object MetaClassLoader {

  val factories : mutable.Map[SimpleClass[?], () => Any] = mutable.Map.empty
  val classes : mutable.Map[Class[?], SimpleClass[?]] = mutable.Map.empty
  val typeNames : mutable.Map[String, SimpleClass[?]] = mutable.Map.empty

  def register(clazz: SimpleClass[?], instance: () => Any): Unit = {
    factories += clazz -> instance
    classes += clazz.runtimeClass -> clazz
    
    val typeName = clazz.annotations
      .collectFirst {
        case Annotation(className, params) if className == "jfx.json.JsonType" =>
          params.getOrElse("value", clazz.typeName).asInstanceOf[String]
      }
      .getOrElse(clazz.typeName)
    
    typeNames += typeName -> clazz
  }

  def getByTypeName(typeName: String): Option[SimpleClass[?]] =
    typeNames.get(typeName)

}
