package jfx.json.serializer

import scala.scalajs.js

class NumberSerializer extends Serializer[Number] {
  override def serialize(input: Number, context: JavaContext): js.Dynamic = input.asInstanceOf[js.Dynamic]
}

class BooleanSerializer extends Serializer[Boolean] {
  override def serialize(input: Boolean, context: JavaContext): js.Dynamic = input.asInstanceOf[js.Dynamic]
}