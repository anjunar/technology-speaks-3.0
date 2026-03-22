package com.anjunar.technologyspeaks.rest.types

import com.anjunar.json.mapper.provider.DTO
import com.anjunar.json.mapper.schema.EntitySchema
import jakarta.json.bind.annotation.JsonbProperty

import scala.beans.BeanProperty

open class Data[E](
  @JsonbProperty @BeanProperty val data: E,
  @JsonbProperty @BeanProperty val schema: EntitySchema[?]
) extends DTO
