package com.anjunar.json.mapper.schema.property

import com.anjunar.json.mapper.schema.{EntitySchema, Helper, Link, SchemaProvider, VisibilityRule}
import com.anjunar.scala.enterprise.macros.{PropertyAccess, TypeHelper}
import com.anjunar.scala.enterprise.macros.reflection.Type
import com.anjunar.scala.universe.TypeResolver
import jakarta.json.bind.annotation.JsonbProperty

import java.lang.annotation.Annotation
import java.util

class Property[T, V](val propertyAccess : PropertyAccess[T, V], val rule: VisibilityRule[T]) {
  
  val name : String = propertyAccess.name
  
  val isWriteable : Boolean = propertyAccess.isWriteable

  val genericType : Type = propertyAccess.genericType
  
  def get(instance: T): V = propertyAccess.get(instance)

  def set(instance: T, value: V): Unit = propertyAccess.set(instance, value)
  
  def findAnnotation[A <: Annotation](clazz: Class[A]): A = {
    val option = propertyAccess.annotations.find(_.annotationType == clazz)
    if (option.isEmpty) {
      null.asInstanceOf[A]
    } else {
      option.get.asInstanceOf[A]
    }
  }

  @JsonbProperty("type")
  val typeName: String = TypeHelper.rawType(propertyAccess.genericType).getSimpleName

  @JsonbProperty
  var schema: EntitySchema[?] =
    val value = TypeHelper.entityTypeResolve(propertyAccess.genericType)

    if (value == null) {
      null
    } else {
      val companion = TypeResolver.companionInstance[SchemaProvider[?]](value)

      if (companion == null) {
        null
      } else {
        companion.schema
      }
    }

  @JsonbProperty("$links")
  val links: util.List[Link] = new util.ArrayList[Link]()

  def addLinks(value: Link*): Unit =
    value.filter(_ != null).foreach(link => links.add(link))

}
