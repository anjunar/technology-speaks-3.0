package com.anjunar.json.mapper.serializers

import com.anjunar.json.mapper.JavaContext
import com.anjunar.json.mapper.intermediate.model.{JsonNode, JsonObject}

class MapSerializer extends Serializer[java.util.Map[String, ?]] {

  override def serialize(input: java.util.Map[String, ?], context: JavaContext): JsonNode = {
    val nodes = new java.util.HashMap[String, JsonNode]()
    val jsonObject = new JsonObject(nodes)

    val iterator = input.entrySet().iterator()
    while (iterator.hasNext) {
      val entry = iterator.next()
      val value = entry.getValue

      val serializer = SerializerRegistry
        .find(context.resolvedClass.typeArguments(1).raw.asInstanceOf[Class[Any]], value)
        .asInstanceOf[Serializer[Any]]

      val javaContext = new JavaContext(
        context.resolvedClass.typeArguments(1),
        context.graph,
        context,
        context.name
      )

      val node = serializer.serialize(value, javaContext)
      nodes.put(entry.getKey, node)
    }

    jsonObject
  }

}
