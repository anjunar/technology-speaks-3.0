package jfx.json.serializer

import scala.scalajs.js

class StringSerializer extends Serializer[String] {
  
  override def serialize(input: String, context: JavaContext): js.Dynamic = input.asInstanceOf[js.Dynamic]
  
}
