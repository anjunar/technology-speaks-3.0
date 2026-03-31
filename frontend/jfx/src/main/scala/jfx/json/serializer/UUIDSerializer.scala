package jfx.json.serializer

import java.util.UUID
import scala.scalajs.js

class UUIDSerializer extends Serializer[UUID] {
  override def serialize(input: UUID, context: JavaContext): js.Dynamic = input.toString.asInstanceOf[js.Dynamic]
}
