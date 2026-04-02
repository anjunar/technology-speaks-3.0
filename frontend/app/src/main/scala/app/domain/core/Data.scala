package app.domain.core

import jfx.json.JsonIgnore

import scala.scalajs.js

class Data[E](
  var data: E = null.asInstanceOf[E],
  var score: Double = 1.0d,
  @JsonIgnore
  var schema: Schema | Null = null
)