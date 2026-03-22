package com.anjunar.technologyspeaks.rest.types

import com.anjunar.json.mapper.provider.DTO
import com.anjunar.json.mapper.schema.EntitySchema
import jakarta.json.bind.annotation.JsonbProperty

import scala.annotation.meta.field
import scala.beans.BeanProperty

class Table[C](
                @(JsonbProperty @field) @BeanProperty val rows: java.util.List[C],
                @(JsonbProperty @field) @BeanProperty val size: Long,
                @(JsonbProperty @field) @BeanProperty val schema: EntitySchema[?]
) extends DTO with LinksContainer.Trait
