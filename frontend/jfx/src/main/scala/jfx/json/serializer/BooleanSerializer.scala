package jfx.json.serializer

import scala.scalajs.js

class BooleanSerializer extends Serializer[Boolean] {
  override def serialize(input: Boolean, context: JavaContext): js.Dynamic = input.asInstanceOf[js.Dynamic]
}
