package com.anjunar.json.mapper

import jakarta.json.bind.annotation.JsonbProperty
import scala.beans.BeanProperty

class ErrorRequest(
  @JsonbProperty @BeanProperty val path: java.util.List[Any],
  @JsonbProperty @BeanProperty val message: String
)
