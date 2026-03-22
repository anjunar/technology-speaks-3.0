package com.anjunar.json.mapper.deserializer

import com.anjunar.json.mapper.annotations.UseConverter
import com.anjunar.json.mapper.provider.{DTO, EntityProvider}
import com.anjunar.json.mapper.schema.{SchemaProvider, VisibilityRule}
import com.anjunar.json.mapper.{JsonContext, ObjectMapperProvider}
import com.anjunar.json.mapper.intermediate.model.{JsonNode, JsonNull, JsonObject}
import com.anjunar.scala.universe.{ResolvedClass, TypeResolver}
import com.anjunar.scala.universe.introspector.{BeanIntrospector, BeanProperty}
import jakarta.persistence.{EntityGraph, ManyToMany, ManyToOne, OneToMany, OneToOne, Subgraph}

import java.lang.reflect.InvocationTargetException
import java.util.UUID

class BeanDeserializer extends Deserializer[Any] {

  override def deserialize(json: JsonNode, context: JsonContext): Any =
    json match {
      case jsonObject: JsonObject =>
        val beanModel = BeanIntrospector.create(context.resolvedClass)

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

          if (property.name == "id") {
            index += 1
          } else if (context.instance != null && context.instance.isInstanceOf[EntityProvider] && context.instance.asInstanceOf[EntityProvider].version > -1L) {
            if (
              property.name != "links" &&
              classOf[EntityProvider].isAssignableFrom(context.resolvedClass.raw) &&
              context.graph != null &&
              !isSelectedByGraph(context, property)
            ) {
              index += 1
            } else {
              handleProperty(jsonObject, context, property, schemaProvider)
              index += 1
            }
          } else {
            handleProperty(jsonObject, context, property, schemaProvider)
            index += 1
          }
        }

        context.instance
      case _ =>
        throw new IllegalArgumentException("json must be a object")
    }

  private def handleProperty(
    json: JsonObject,
    context: JsonContext,
    property: BeanProperty,
    schemaProvider: SchemaProvider
  ): Unit = {
    if (schemaProvider != null) {
      val schemaProperties = schemaProvider.schema().properties
      val schemaProperty = schemaProperties.get(property.name)
      if (schemaProperty == null) {
        return
      }

      val visibilityRule = schemaProperty.rule.asInstanceOf[VisibilityRule[Any]]
      if (!visibilityRule.isWriteable(context.instance, property)) {
        return
      }
    }

    val propertyType = property.propertyType.raw

    val oldValue =
      try {
        if (context.instance == null) {
          null
        } else {
          property.get(context.instance.asInstanceOf[AnyRef])
        }
      } catch {
        case exception: InvocationTargetException =>
          if (exception.getCause != null && exception.getCause.getClass.getSimpleName != "UninitializedPropertyAccessException") {
            throw exception.getCause
          } else {
            null
          }
      }

    val node = json.value.get(property.name)

    if (classOf[DTO].isAssignableFrom(propertyType)) {
      handleEntityProperty(node, property, context, oldValue, propertyType)
    } else if (classOf[java.util.Collection[?]].isAssignableFrom(propertyType)) {
      handleCollectionProperty(node, property, context, oldValue)
    } else if (classOf[java.util.Map[?, ?]].isAssignableFrom(propertyType)) {
      handleMapProperty(node, property, context, oldValue)
    } else {
      handleNormalProperty(node, property, context, oldValue)
    }
  }

  private def handleNormalProperty(
    node: JsonNode,
    property: BeanProperty,
    context: JsonContext,
    oldValue: Any
  ): Unit = {
    if (node == null) {
      return
    }

    if (node.isInstanceOf[JsonNull]) {
      if (context.instance != null) {
        context.checkForViolations(context.instance.getClass, property.name, null, () => property.set(context.instance.asInstanceOf[AnyRef], null))
      }
      return
    }

    val instance = context.instance
    val useConverter = property.findAnnotation(classOf[UseConverter])

    if (useConverter == null) {
      val value = deserializeValue(property.propertyType, property.name, oldValue, context, node)
      context.checkForViolations(instance.getClass, property.name, value, () => property.set(instance.asInstanceOf[AnyRef], value))
    } else {
      val rawValue = deserializeValue(TypeResolver.resolve(classOf[String]), property.name, oldValue, context, node)
      if (!rawValue.isInstanceOf[String]) {
        throw new IllegalArgumentException("Converter only support string type")
      }

      val converter = useConverter.value().getDeclaredConstructor().newInstance()
      val convertedValue = converter.toJava(rawValue.asInstanceOf[String], property.propertyType)

      context.checkForViolations(instance.getClass, property.name, convertedValue, () => property.set(instance.asInstanceOf[AnyRef], convertedValue))
    }
  }

  private def handleCollectionProperty(
    node: JsonNode,
    property: BeanProperty,
    context: JsonContext,
    oldValue: Any
  ): Unit = {
    if (node == null) {
      return
    }

    val instance = context.instance
    val existingCollection =
      if (oldValue != null) {
        oldValue
      } else {
        throw new IllegalStateException("Collection property must be initialized")
      }

    val deserialized = deserializeValue(property.propertyType, property.name, existingCollection, context, node).asInstanceOf[java.util.Collection[Any]]
    val targetCollection = property.get(instance.asInstanceOf[AnyRef]).asInstanceOf[java.util.Collection[Any]]

    context.checkForViolations(instance.getClass, property.name, targetCollection, () => {
      targetCollection.clear()
      targetCollection.addAll(deserialized)
      synchronizeBidirectionalRelations(instance, property, targetCollection)
    })
  }

  private def handleMapProperty(
    node: JsonNode,
    property: BeanProperty,
    context: JsonContext,
    oldValue: Any
  ): Unit = {
    if (node == null) {
      return
    }

    val instance = context.instance
    val existingMap =
      if (oldValue != null) {
        oldValue
      } else {
        throw new IllegalStateException("Collection property must be initialized")
      }

    val deserialized = deserializeValue(property.propertyType, property.name, existingMap, context, node).asInstanceOf[java.util.Map[String, Any]]
    val targetMap = property.get(instance.asInstanceOf[AnyRef]).asInstanceOf[java.util.Map[String, Any]]

    context.checkForViolations(instance.getClass, property.name, targetMap, () => {
      targetMap.clear()
      targetMap.putAll(deserialized)
      synchronizeBidirectionalRelations(instance, property, targetMap)
    })
  }

  private def handleEntityProperty(
    node: JsonNode,
    property: BeanProperty,
    context: JsonContext,
    oldValue: Any,
    propertyType: Class[?]
  ): Unit = {
    if (node == null) {
      return
    }

    val instance = context.instance

    if (node.isInstanceOf[JsonNull]) {
      context.checkForViolations(instance.getClass, property.name, null, () => property.set(instance.asInstanceOf[AnyRef], null))
      return
    }

    if (oldValue != null) {
      val value = deserializeValue(property.propertyType, property.name, oldValue, context, node)
      context.checkForViolations(instance.getClass, property.name, value, () => setPropertyAndSynchronize(instance, property, value))
      return
    }

    val jsonObject = node.asInstanceOf[JsonObject]
    val jsonId = jsonObject.value.get("id")

    if (jsonId != null) {
      val id = UUID.fromString(jsonId.value.toString)
      val entity = context.loader.load(id, propertyType)
      if (entity != null) {
        context.checkForViolations(instance.getClass, property.name, entity, () => property.set(instance.asInstanceOf[AnyRef], entity))
        return
      }
    }

    val value = deserializeNewEntity(propertyType, property, context, node)
    context.checkForViolations(instance.getClass, property.name, value, () => setPropertyAndSynchronize(instance, property, value))
  }

  private def deserializeNewEntity(
    propertyType: Class[?],
    property: BeanProperty,
    context: JsonContext,
    node: JsonNode
  ): Any = {
    val newInstance = propertyType.getConstructor().newInstance()
    deserializeValue(property.propertyType, property.name, newInstance, context, node)
  }

  private def setPropertyAndSynchronize(owner: Any, property: BeanProperty, value: Any): Unit = {
    property.set(owner.asInstanceOf[AnyRef], value)
    synchronizeBidirectionalRelations(owner, property, value)
  }

  private def synchronizeBidirectionalRelations(owner: Any, property: BeanProperty, value: Any): Unit = {
    if (value == null) {
      return
    }

    val oneToOne = property.findAnnotation(classOf[OneToOne])
    if (oneToOne != null) {
      synchronizeOneToOne(owner, property, value, oneToOne)
      return
    }

    val oneToMany = property.findAnnotation(classOf[OneToMany])
    if (oneToMany != null) {
      synchronizeOneToMany(owner, value.asInstanceOf[java.lang.Iterable[?]], oneToMany)
      return
    }

    val manyToOne = property.findAnnotation(classOf[ManyToOne])
    if (manyToOne != null) {
      synchronizeManyToOne(owner, property, value)
      return
    }

    val manyToMany = property.findAnnotation(classOf[ManyToMany])
    if (manyToMany != null) {
      synchronizeManyToMany(owner, property, value.asInstanceOf[java.lang.Iterable[?]], manyToMany)
    }
  }

  private def synchronizeOneToOne(owner: Any, property: BeanProperty, value: Any, oneToOne: OneToOne): Unit = {
    val mappedBy = oneToOne.mappedBy()
    val otherModel = BeanIntrospector.createWithType(value.getClass)

    if (!mappedBy.isBlank) {
      val owningSide = otherModel.findProperty(mappedBy)
      if (owningSide != null) {
        setRelationIfNeeded(value, owningSide, owner)
      }
      return
    }

    val inverseSide = resolveInverseOneToOne(value, property.name, owner.getClass)
    if (inverseSide != null) {
      setRelationIfNeeded(value, inverseSide, owner)
    }
  }

  private def synchronizeOneToMany(owner: Any, values: java.lang.Iterable[?], oneToMany: OneToMany): Unit = {
    val mappedBy = oneToMany.mappedBy()
    if (mappedBy.isBlank) {
      return
    }

    val iterator = values.iterator()
    while (iterator.hasNext) {
      val element = iterator.next()
      if (element != null) {
        val elementModel = BeanIntrospector.createWithType(element.getClass)
        val owningSide = elementModel.findProperty(mappedBy)
        if (owningSide != null) {
          setRelationIfNeeded(element, owningSide, owner)
        }
      }
    }
  }

  private def synchronizeManyToOne(owner: Any, property: BeanProperty, value: Any): Unit = {
    val inverseCollection = resolveInverseOneToMany(value, property.name, owner.getClass)
    if (inverseCollection != null) {
      addToCollectionIfNeeded(value, inverseCollection, owner)
    }
  }

  private def synchronizeManyToMany(owner: Any, property: BeanProperty, values: java.lang.Iterable[?], manyToMany: ManyToMany): Unit = {
    val mappedBy = manyToMany.mappedBy()
    val iterator = values.iterator()

    while (iterator.hasNext) {
      val element = iterator.next()
      if (element != null) {
        val otherModel = BeanIntrospector.createWithType(element.getClass)

        val otherSideProperty =
          if (!mappedBy.isBlank) {
            otherModel.findProperty(mappedBy)
          } else {
            resolveInverseManyToMany(element, property.name, owner.getClass)
          }

        if (otherSideProperty != null) {
          addToCollectionIfNeeded(element, otherSideProperty, owner)
        }
      }
    }
  }

  private def setRelationIfNeeded(instance: Any, property: BeanProperty, value: Any): Unit = {
    if (!property.isWriteable) {
      return
    }

    if (!property.propertyType.raw.isAssignableFrom(value.getClass)) {
      return
    }

    val existing =
      try {
        property.get(instance.asInstanceOf[AnyRef])
      } catch {
        case _: Exception => null
      }

    if (existing.asInstanceOf[AnyRef] ne value.asInstanceOf[AnyRef]) {
      property.set(instance.asInstanceOf[AnyRef], value)
    }
  }

  private def addToCollectionIfNeeded(instance: Any, property: BeanProperty, value: Any): Unit = {
    if (!classOf[java.util.Collection[?]].isAssignableFrom(property.propertyType.raw)) {
      return
    }

    val elementType = property.propertyType.typeArguments.headOption.map(_.raw).orNull
    if (elementType != null && !elementType.isAssignableFrom(value.getClass)) {
      return
    }

    val collection =
      try {
        property.get(instance.asInstanceOf[AnyRef]).asInstanceOf[java.util.Collection[Any]]
      } catch {
        case _: Exception => null
      }

    if (collection != null && !collection.contains(value)) {
      collection.add(value)
    }
  }

  private def resolveInverseOneToOne(target: Any, mappedBy: String, expectedType: Class[?]): BeanProperty = {
    val targetModel = BeanIntrospector.createWithType(target.getClass)
    targetModel.properties.find { candidate =>
      val annotation = candidate.findAnnotation(classOf[OneToOne])
      annotation != null &&
      annotation.mappedBy() == mappedBy &&
      candidate.propertyType.raw.isAssignableFrom(expectedType)
    }.orNull
  }

  private def resolveInverseOneToMany(target: Any, mappedBy: String, expectedElementType: Class[?]): BeanProperty = {
    val targetModel = BeanIntrospector.createWithType(target.getClass)
    targetModel.properties.find { candidate =>
      val annotation = candidate.findAnnotation(classOf[OneToMany])
      val elementType = candidate.propertyType.typeArguments.headOption.map(_.raw).orNull
      annotation != null &&
      annotation.mappedBy() == mappedBy &&
      (elementType == null || elementType.isAssignableFrom(expectedElementType))
    }.orNull
  }

  private def resolveInverseManyToMany(target: Any, mappedBy: String, expectedElementType: Class[?]): BeanProperty = {
    val targetModel = BeanIntrospector.createWithType(target.getClass)
    targetModel.properties.find { candidate =>
      val annotation = candidate.findAnnotation(classOf[ManyToMany])
      val elementType = candidate.propertyType.typeArguments.headOption.map(_.raw).orNull
      annotation != null &&
      annotation.mappedBy() == mappedBy &&
      (elementType == null || elementType.isAssignableFrom(expectedElementType))
    }.orNull
  }

  private def deserializeValue(
    propertyType: ResolvedClass,
    name: String,
    existingInstance: Any,
    context: JsonContext,
    node: JsonNode
  ): Any = {
    val deserializer = DeserializerRegistry.findDeserializer(propertyType.raw.asInstanceOf[Class[Any]], node)
    val jsonContext = new JsonContext(propertyType, existingInstance, context.graph, context.loader, context.validator, context, name)
    deserializer.deserialize(node, jsonContext)
  }

  private def isSelectedByGraph(context: JsonContext, property: BeanProperty): Boolean = {
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

  private def resolveContainer(context: JsonContext): Any = {
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

  private def findSubgraph(context: JsonContext): Subgraph[?] = {
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
