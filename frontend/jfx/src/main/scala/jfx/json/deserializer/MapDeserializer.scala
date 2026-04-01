package jfx.json.deserializer

import com.anjunar.scala.enterprise.macros.reflection.{ParameterizedType, Type}

import scala.scalajs.js
import scala.scalajs.js.Dynamic

class MapDeserializer extends Deserializer[Map[?, ?]] {

  override def deserialize(json: Dynamic, context: JsonContext): Any = {
    val elemType = context.resolvedType match {
      case pt: ParameterizedType if pt.typeArguments.length >= 2 => pt.typeArguments(1)
      case _ => throw new IllegalStateException("Map must have two type arguments")
    }

    val jsonObj = json.asInstanceOf[js.Dynamic]
    val keys = js.Dynamic.global.Object.keys(jsonObj).asInstanceOf[js.Array[String]]
    
    val builder = Map.newBuilder[String, Any]
    var i = 0
    while (i < keys.length) {
      val key = keys(i)
      val elemJson = jsonObj.selectDynamic(key).asInstanceOf[Dynamic]
      val elemContext = new JsonContext(elemType)
      val deserializer = DeserializerFactory.buildFromType(elemType)
      val value = deserializer.deserialize(elemJson, elemContext)
      builder += ((key, value))
      i += 1
    }
    builder.result()
  }

}
