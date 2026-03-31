package jfx.json.serializer

import scala.scalajs.js.Dynamic


trait Serializer[T] {
  def serialize(input: T, context: JavaContext): Dynamic
}
