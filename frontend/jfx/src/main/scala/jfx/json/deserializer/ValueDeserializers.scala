package jfx.json.deserializer

import scala.scalajs.js
import scala.scalajs.js.Dynamic

class StringDeserializer extends Deserializer[String] {
  override def deserialize(json: Dynamic, context: JsonContext): Any = {
    if (json == null || js.isUndefined(json)) null
    else json.toString
  }
}

class NumberDeserializer extends Deserializer[Number] {
  override def deserialize(json: Dynamic, context: JsonContext): Any = {
    val typeName = context.resolvedType.typeName
    val doubleValue = json.asInstanceOf[Double]
    typeName match {
      case "scala.Long" | "Long" => doubleValue.toLong
      case "scala.Float" | "Float" => doubleValue.toFloat
      case "scala.Int" | "Int" => doubleValue.toInt
      case _ => doubleValue
    }
  }
}

class BooleanDeserializer extends Deserializer[Boolean] {
  override def deserialize(json: Dynamic, context: JsonContext): Any = json.asInstanceOf[Boolean]
}

class UUIDDeserializer extends Deserializer[java.util.UUID] {
  override def deserialize(json: Dynamic, context: JsonContext): Any = java.util.UUID.fromString(json.toString)
}
