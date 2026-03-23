package com.anjunar.json.mapper.serializers

import com.anjunar.json.mapper.annotations.UseConverter
import com.anjunar.json.mapper.provider.EntityProvider
import com.anjunar.json.mapper.schema.{SchemaProvider, VisibilityRule}
import com.anjunar.json.mapper.{JavaContext, ObjectMapperProvider}
import com.anjunar.json.mapper.intermediate.model.{JsonNode, JsonObject, JsonString}
import com.anjunar.scala.universe.TypeResolver
import com.anjunar.scala.universe.introspector.{AbstractProperty, AnnotationIntrospector, AnnotationProperty}
import jakarta.json.bind.annotation.{JsonbProperty, JsonbSubtype}
import jakarta.persistence.{Entity, EntityGraph, Subgraph}

class BeanSerializer extends Serializer[Any] {

  override def serialize(input: Any, context: JavaContext): JsonNode = {
    val beanModel = AnnotationIntrospector.create(context.resolvedClass, classOf[JsonbProperty])
    val nodes = new java.util.LinkedHashMap[String, JsonNode]()
    val json = new JsonObject(nodes)

    val companionInstance = TypeResolver.companionInstance(context.resolvedClass.raw)
    val schemaProvider =
      if (companionInstance != null && companionInstance.isInstanceOf[SchemaProvider]) {
        companionInstance.asInstanceOf[SchemaProvider]
      } else {
        null
      }

    val properties = beanModel.properties
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
          val schemaProperties = schemaProvider.schema().properties
          val schemaProperty = schemaProperties.get(property.name)

          if (schemaProperty == null) {
            index += 1
          } else {
            val visibilityRule = schemaProperty.rule.asInstanceOf[VisibilityRule[Any]]
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
                                 property: AnnotationProperty
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

  private def convertToJsonNode(property: AbstractProperty,
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
        classOf[java.util.Collection[?]].isAssignableFrom(property.propertyType.raw) ||
          classOf[java.util.Map[?, ?]].isAssignableFrom(property.propertyType.raw)
      ) {
        property.propertyType
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
        val serializer = SerializerRegistry.find(property.propertyType.raw.asInstanceOf[Class[Any]], value).asInstanceOf[Serializer[Any]]
        serializer.serialize(value, javaContext)
      } else {
        val converter = converterAnnotation.value().getDeclaredConstructor().newInstance()
        val toJson = converter.toJson(value, property.propertyType)
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

  private def isSelectedByGraph(context: JavaContext, property: AnnotationProperty): Boolean = {
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
