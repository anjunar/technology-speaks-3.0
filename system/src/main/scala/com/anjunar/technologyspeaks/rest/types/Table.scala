package com.anjunar.technologyspeaks.rest.types

import com.anjunar.json.mapper.provider.DTO
import com.anjunar.json.mapper.schema.EntitySchema
import jakarta.json.bind.annotation.JsonbProperty

import scala.annotation.meta.field

class Table[C](
                @(JsonbProperty @field) val rows: java.util.List[C],
                @(JsonbProperty @field) val size: Long,
                @(JsonbProperty @field) val schema: EntitySchema[?]
) extends DTO with LinksContainer
