package com.anjunar.technologyspeaks.rest.types

import com.anjunar.json.mapper.provider.DTO
import com.anjunar.json.mapper.schema.EntitySchema
import jakarta.json.bind.annotation.JsonbProperty

import scala.annotation.meta.field

open class Data[E](
  @(JsonbProperty @field) val data: E,
  @(JsonbProperty @field) val schema: EntitySchema[?]
) extends DTO
