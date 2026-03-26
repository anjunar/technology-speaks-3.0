package com.anjunar.technologyspeaks.rest.types

import com.anjunar.json.mapper.provider.DTO
import com.anjunar.json.mapper.schema.EntitySchema
import jakarta.json.bind.annotation.JsonbProperty

import scala.annotation.meta.field

class Data[E](@(JsonbProperty @field) val data: E,
              @(JsonbProperty @field)("schema") val dataSchema: EntitySchema[?]) extends DTO