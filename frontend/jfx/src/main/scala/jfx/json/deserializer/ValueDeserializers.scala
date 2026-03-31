package jfx.json.deserializer

import scala.scalajs.js
import scala.scalajs.js.Dynamic

class StringDeserializer extends Deserializer[String] {
  override def deserialize(json: Dynamic, context: JsonContext): Any = json.toString
}

class NumberDeserializer extends Deserializer[Number] {
  override def deserialize(json: Dynamic, context: JsonContext): Any = json.asInstanceOf[Double]
}

class BooleanDeserializer extends Deserializer[Boolean] {
  override def deserialize(json: Dynamic, context: JsonContext): Any = json.asInstanceOf[Boolean]
}

class UUIDDeserializer extends Deserializer[java.util.UUID] {
  override def deserialize(json: Dynamic, context: JsonContext): Any = java.util.UUID.fromString(json.toString)
}
