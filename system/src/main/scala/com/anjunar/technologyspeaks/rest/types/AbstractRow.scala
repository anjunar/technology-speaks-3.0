package com.anjunar.technologyspeaks.rest.types

import com.anjunar.json.mapper.schema.{EntitySchema, SchemaProvider}
import com.anjunar.scala.universe.TypeResolver
import jakarta.json.bind.annotation.JsonbProperty

import scala.annotation.meta.field
import scala.beans.BeanProperty

abstract class AbstractRow[E](
  @(JsonbProperty @field) @BeanProperty val data: E,
  clazz: Class[E]
) extends LinksContainer.Trait {

  @JsonbProperty @BeanProperty
  val schema: EntitySchema[?] =
    TypeResolver.companionInstance(clazz).asInstanceOf[SchemaProvider].schema()

}
