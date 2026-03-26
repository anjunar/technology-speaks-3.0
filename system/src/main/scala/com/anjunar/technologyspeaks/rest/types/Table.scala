package com.anjunar.technologyspeaks.rest.types

import com.anjunar.json.mapper.provider.DTO
import com.anjunar.json.mapper.schema.{EntitySchema, SchemaProvider}
import com.anjunar.technologyspeaks.rest.types.Table.Schema
import jakarta.json.bind.annotation.JsonbProperty

import scala.annotation.meta.field

class Table[C](
                @(JsonbProperty @field) val rows: java.util.List[C],
                @(JsonbProperty @field) val size: Long,
                @(JsonbProperty @field)("schema") val tableSchema: EntitySchema[?]
) extends DTO with LinksContainer

object Table extends SchemaProvider[Schema] {
  
  class Schema extends EntitySchema[Table[?]]
  
}
