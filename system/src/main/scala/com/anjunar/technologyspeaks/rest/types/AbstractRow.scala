package com.anjunar.technologyspeaks.rest.types

import com.anjunar.json.mapper.schema.{EntitySchema, SchemaProvider}
import com.anjunar.scala.universe.TypeResolver
import jakarta.json.bind.annotation.JsonbProperty

import scala.annotation.meta.field

abstract class AbstractRow[E](
  @(JsonbProperty @field) val data: E,
  clazz: Class[E]
) extends LinksContainer {

  @JsonbProperty
  val schema: EntitySchema[?] =
    TypeResolver.companionInstance(clazz).asInstanceOf[SchemaProvider[?]].schema

}
