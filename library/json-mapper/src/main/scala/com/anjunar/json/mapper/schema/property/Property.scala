package com.anjunar.json.mapper.schema.property

import com.anjunar.json.mapper.macros.PropertyAccess
import com.anjunar.json.mapper.schema.{EntitySchema, Helper, SchemaProvider, VisibilityRule}
import com.anjunar.scala.universe.TypeResolver
import jakarta.json.bind.annotation.JsonbProperty

import java.lang.reflect.Type

class Property[T, V](val propertyAccess : PropertyAccess[T, V], var rule: VisibilityRule[T]) {

  def get(instance: T): V = propertyAccess.get(instance)

  def set(instance: T, value: V): Unit = propertyAccess.set(instance, value)

  @JsonbProperty("type")
  val typeName: String = TypeResolver.rawType(propertyAccess.genericType).getSimpleName

  @JsonbProperty
  val schema: EntitySchema[?] =
    val value = Helper.entityType(propertyAccess.genericType)

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




}