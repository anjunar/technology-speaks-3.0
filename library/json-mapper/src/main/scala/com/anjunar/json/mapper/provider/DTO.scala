package com.anjunar.json.mapper.provider

import com.anjunar.json.mapper.schema.{EntitySchema, SchemaProvider}
import com.anjunar.scala.universe.TypeResolver

trait DTO {
  
  def schemaFor[E](entityClass : Class[E]) : EntitySchema[E] = TypeResolver.companionInstance[SchemaProvider[EntitySchema[E]]](entityClass).schema
  
}
