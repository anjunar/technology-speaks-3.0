package app.domain.core

import app.support.JsonModel
import jfx.core.meta.Meta
import jfx.json.JsonIgnore

import scala.scalajs.js
import scala.reflect.ClassTag

class Table[E: ClassTag](
  var rows: js.Array[E] = new js.Array[E](),
  var size: Int = 0,
  var schema: Schema | Null = null
) 