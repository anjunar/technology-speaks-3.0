package jfx.json.serializer

import jfx.form.Model

import scala.scalajs.js
import scala.scalajs.js.Dynamic

class MapSerializer extends Serializer[Map[?, ?]] {

  override def serialize(input: Map[?, ?], context: JavaContext): Dynamic = {
    val out = js.Dictionary[js.Any]()

    input.foreach { case (key, value) =>
      val serialized = serializeValue(value)
      out.update(key.toString, serialized)
    }

    out.asInstanceOf[js.Dynamic]
  }

  private def serializeValue(value: Any): js.Any = {
    value match {
      case null => null
      case m: Model[?] =>
        val serializer = new ModelSerializer()
        serializer.serialize(m, new JavaContext(null))
      case arr: js.Array[?] => arr.map(serializeValue)
      case map: Map[?, ?] => serialize(map, new JavaContext(null))
      case v => value.asInstanceOf[js.Any]
    }
  }

}
