package com.anjunar.technologyspeaks.rest.types

import com.anjunar.json.mapper.macros.PropertyMacrosHelper
import com.anjunar.json.mapper.provider.DTO
import com.anjunar.json.mapper.schema.property.Property
import com.anjunar.json.mapper.schema.{EntitySchema, SchemaProvider}
import jakarta.json.bind.annotation.JsonbProperty

import scala.annotation.meta.field
import scala.collection.mutable

class Data[E](@(JsonbProperty @field) val data: E,
              @(JsonbProperty @field)("schema") val dataSchema: EntitySchema[?]) extends DTO