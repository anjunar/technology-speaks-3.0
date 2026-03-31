package jfx.json.deserializer

import scala.scalajs.js.Dynamic

trait Deserializer[T] {
  def deserialize(json: Dynamic, context: JsonContext): Any
}
