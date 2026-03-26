package com.anjunar.json.mapper.serializers

import com.anjunar.json.mapper.annotations.UseConverter
import com.anjunar.json.mapper.provider.{DTO, EntityProvider}
import com.anjunar.json.mapper.schema.{EntitySchema, SchemaProvider, VisibilityRule}
import com.anjunar.json.mapper.{JavaContext, ObjectMapperProvider}
import com.anjunar.json.mapper.intermediate.model.{JsonNode, JsonObject, JsonString}
import com.anjunar.json.mapper.schema.property.Property
import com.anjunar.scala.universe.TypeResolver
import jakarta.json.bind.annotation.{JsonbProperty, JsonbSubtype}
import jakarta.persistence.{Entity, EntityGraph, Subgraph}

class BeanSerializer extends Serializer[DTO] {

  override def serialize(input: DTO, context: JavaContext): JsonNode = {
    val nodes = new java.util.LinkedHashMap[String, JsonNode]()
    val json = new JsonObject(nodes)

    val schemaProvider : EntitySchema[Any] = input.schema
    val properties : Array[Property[Any, Any]] = schemaProvider.properties.values.toArray.asInstanceOf[Array[Property[Any, Any]]]

    var index = 0
    while (index < properties.length) {
      val property = properties(index)

      if (
        property.name != "links" &&
          classOf[EntityProvider].isAssignableFrom(context.resolvedClass.raw) &&
          context.graph != null &&
          !isSelectedByGraph(context, property)
      ) {
        index += 1
      } else {
        if (schemaProvider != null) {
          val schemaProperties = schemaProvider.properties
          val schemaProperty = schemaProperties(property.name)

          if (schemaProperty == null) {
            index += 1
          } else {
            val visibilityRule = schemaProperty.rule
            if (!visibilityRule.isVisible(input, property)) {
              index += 1
            } else {
              serializeProperty(input, context, nodes, property)
              index += 1
            }
          }
        } else {
          serializeProperty(input, context, nodes, property)
          index += 1
        }
      }
    }

    if (!json.value.isEmpty) {
      val subtype = input.getClass.getAnnotation(classOf[JsonbSubtype])
      val typeProperty = if (subtype != null) {
        new JsonString(subtype.alias())
      } else {
        new JsonString(input.getClass.getSimpleName.replace("$HibernateProxy", ""))
      }
      json.value.putIfAbsent("@type", typeProperty)

    }

    json
  }

  private def serializeProperty(
                                 input: Any,
                                 context: JavaContext,
                                 nodes: java.util.LinkedHashMap[String, JsonNode],
                                 property: Property[Any, Any]
                               ): Unit = {
    val value =
      try {
        property.get(input.asInstanceOf[AnyRef])
      } catch {
        case _: Exception => null
      }

    value match {
      case booleanValue: java.lang.Boolean =>
        if (booleanValue.booleanValue()) {
          convertToJsonNode(property, nodes, booleanValue, context)
        }
      case booleanValue: Boolean =>
        if (booleanValue) {
          convertToJsonNode(property, nodes, Boolean.box(booleanValue), context)
        }
      case stringValue: String =>
        if (!stringValue.isEmpty) {
          convertToJsonNode(property, nodes, stringValue, context)
        }
      case collectionValue: java.util.Collection[?] =>
        if (!collectionValue.isEmpty) {
          convertToJsonNode(property, nodes, collectionValue, context)
        }
      case _ =>
        if (value != null) {
          convertToJsonNode(property, nodes, value, context)
        }
    }
  }

  private def convertToJsonNode(property: Property[Any, Any],
                                nodes: java.util.LinkedHashMap[String, JsonNode],
                                value: Any,
                                context: JavaContext): Unit = {
    val jsonbProperty = property.findAnnotation(classOf[JsonbProperty])
    if (jsonbProperty == null) {
      return
    }

    val name =
      if (jsonbProperty.value().isEmpty) property.name else jsonbProperty.value()

    val propertyType =
      if (
        classOf[java.util.Collection[?]].isAssignableFrom(property.propertyType) ||
          classOf[java.util.Map[?, ?]].isAssignableFrom(property.propertyType)
      ) {
        TypeResolver.resolve(property.propertyType)
      } else {
        TypeResolver.resolve(value.getClass)
      }

    val javaContext = new JavaContext(
      propertyType,
      context.graph,
      context,
      property.name
    )

    val converterAnnotation = property.findAnnotation(classOf[UseConverter])

    val jsonNode =
      if (converterAnnotation == null) {
        val serializer = SerializerRegistry.find(property.propertyType.asInstanceOf[Class[Any]], value).asInstanceOf[Serializer[Any]]
        serializer.serialize(value, javaContext)
      } else {
        val converter = converterAnnotation.value().getDeclaredConstructor().newInstance()
        val toJson = converter.toJson(value, TypeResolver.resolve(property.propertyType))
        val serializer = SerializerRegistry.find(classOf[String].asInstanceOf[Class[Any]], toJson).asInstanceOf[Serializer[Any]]
        serializer.serialize(toJson, javaContext)
      }

    jsonNode match {
      case value: JsonObject =>
        if (!value.value.isEmpty) {
          nodes.put(name, jsonNode)
        }
      case _ =>
        nodes.put(name, jsonNode)
    }
  }

  private def isSelectedByGraph(context: JavaContext, property: Property[Any, Any]): Boolean = {
    val currentContainer = resolveContainer(context)

    if (currentContainer != null) {
      val attributeNodes =
        currentContainer match {
          case value: EntityGraph[?] => value.getAttributeNodes
          case value: Subgraph[?] => value.getAttributeNodes
          case _ => java.util.Collections.emptyList()
        }

      val iterator = attributeNodes.iterator()
      while (iterator.hasNext) {
        if (iterator.next().getAttributeName == property.name) {
          return true
        }
      }
      false
    } else {
      true
    }
  }

  private def resolveContainer(context: JavaContext): Any = {
    if (context.parent == null) {
      return context.graph
    }

    if (
      classOf[java.util.Collection[?]].isAssignableFrom(context.parent.resolvedClass.raw) ||
        context.parent.resolvedClass.raw.isArray
    ) {
      return resolveContainer(context.parent)
    }

    if (!classOf[EntityProvider].isAssignableFrom(context.parent.resolvedClass.raw)) {
      return context.graph
    }

    findSubgraph(context)
  }

  private def findSubgraph(context: JavaContext): Subgraph[?] = {
    val parent = context.parent
    if (parent == null) {
      return null
    }

    val parentContainer = resolveContainer(parent)
    val nodes =
      parentContainer match {
        case value: EntityGraph[?] => value.getAttributeNodes
        case value: Subgraph[?] => value.getAttributeNodes
        case _ => null
      }

    if (nodes == null) {
      return null
    }

    val iterator = nodes.iterator()
    while (iterator.hasNext) {
      val node = iterator.next()
      if (node.getAttributeName == context.name) {
        val subgraphs = node.getSubgraphs.values().iterator()
        if (subgraphs.hasNext) {
          return subgraphs.next()
        }
      }
    }

    null
  }

}
