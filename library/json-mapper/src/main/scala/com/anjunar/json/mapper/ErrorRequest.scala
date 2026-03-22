package com.anjunar.json.mapper

import jakarta.json.bind.annotation.JsonbProperty
import scala.annotation.meta.field
import scala.beans.BeanProperty

class ErrorRequest(
  @(JsonbProperty @field) @BeanProperty val path: java.util.List[Any],
  @(JsonbProperty @field) @BeanProperty val message: String
)
