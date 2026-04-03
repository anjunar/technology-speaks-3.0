package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.provider.OwnerProvider
import com.anjunar.json.mapper.schema.property.Property
import com.anjunar.json.mapper.schema.{EntitySchema, Link, VisibilityRule}
import com.anjunar.technologyspeaks.SpringContext
import com.anjunar.technologyspeaks.security.IdentityHolder
import jakarta.persistence.EntityManager

object SchemaHateoas {
  
  val entityManager: EntityManager = SpringContext.entityManager()
  given EntityManager = entityManager

  private def bootstrapService: ManagedPropertyBootstrapService =
    SpringContext.getBean(classOf[ManagedPropertyBootstrapService])

  def enhance[E](entity: E, schema: EntitySchema[?]): EntitySchema[?] = {
    if (schema == null) {
      return null
    }

    val identityHolder =
      try {
        SpringContext.getBean(classOf[IdentityHolder])
      } catch {
        case _: Throwable => null
      }

    if (identityHolder == null || identityHolder.user == null || identityHolder.user.id == null) {
      schema
    } else {
      cloneSchema(
        schema.asInstanceOf[EntitySchema[Any]],
        entity,
        resolveOwner(entity.asInstanceOf[Any]),
        identityHolder.user
      )
    }
  }

  private def cloneSchema(
    source: EntitySchema[Any],
    instance: Any,
    inheritedOwner: User,
    currentUser: User
  ): EntitySchema[Any] = {
    val copy = instantiateSchemaCopy(source)
    val currentOwner = Option(resolveOwner(instance)).getOrElse(inheritedOwner)

    source.properties.foreach { (name, property) =>
      val nestedSchemaInstance = extractNestedInstance(property, instance)
      val nestedOwner = Option(resolveOwner(nestedSchemaInstance)).getOrElse(currentOwner)
      val clonedProperty = cloneProperty(property, currentOwner, nestedSchemaInstance, nestedOwner, currentUser)
      copy.properties.put(name, clonedProperty.asInstanceOf[Property[Any, Any]])
      assignSchemaField(copy, name, clonedProperty)
    }

    copy
  }

  private def cloneProperty(
    source: Property[Any, ?],
    containerOwner: User,
    nestedSchemaInstance: Any,
    nestedOwner: User,
    currentUser: User
  ): Property[Any, ?] = {
    val copy = new Property[Any, Any](
      source.propertyAccessor.asInstanceOf[_root_.reflect.PropertyAccessor[Any, Any]],
      source.propertyDescriptor,
      source.rule.asInstanceOf[Class[VisibilityRule[Any]]]
    )

    copy.schema =
      if (source.schema == null) null
      else cloneSchema(source.schema.asInstanceOf[EntitySchema[Any]], nestedSchemaInstance, nestedOwner, currentUser)

    if (containerOwner != null && containerOwner.id == currentUser.id) {
      val managedProperty = findOrCreateManagedProperty(containerOwner, source.name)
      if (managedProperty != null && managedProperty.id != null) {
        copy.addLinks(
          new Link("property", s"/core/properties/property/${managedProperty.id}", "GET", null),
          new Link("updateProperty", "/core/properties/property", "PUT", null)
        )
      }
    }

    copy
  }

  private def extractNestedInstance(source: Property[Any, ?], instance: Any): Any = {
    if (instance == null) {
      return null
    }

    val value =
      try {
        source.get(instance)
      } catch {
        case _: Throwable => null
      }

    value.asInstanceOf[Any] match {
      case null => null
      case collection: java.util.Collection[?] =>
        val iterator = collection.iterator()
        if (iterator.hasNext) iterator.next() else null
      case map: java.util.Map[?, ?] =>
        val iterator = map.values().iterator()
        if (iterator.hasNext) iterator.next() else null
      case other => other
    }
  }

  private def resolveOwner(instance: Any): User = {
    instance match {
      case null => null
      case user: User => user
      case ownerProvider: OwnerProvider =>
        val owner = ownerProvider.owner()
        if (owner == null || owner.id == null) null
        else User.find(owner.id)
      case _ => null
    }
  }

  private def findOrCreateManagedProperty(owner: User, propertyName: String): ManagedProperty = {
    if (owner == null || owner.id == null) {
      return null
    }

    bootstrapService.findOrCreateManagedProperty(owner.id, propertyName)
  }

  private def instantiateSchemaCopy(source: EntitySchema[Any]): EntitySchema[Any] = {
    val ctor = source.getClass.getDeclaredConstructors.head
    ctor.setAccessible(true)

    if (ctor.getParameterCount != 0) {
      throw new IllegalStateException(s"Unsupported schema constructor for ${source.getClass.getName}")
    }

    ctor.newInstance().asInstanceOf[EntitySchema[Any]]
  }

  private def assignSchemaField(schema: AnyRef, fieldName: String, value: Any): Unit = {
    var currentClass: Class[?] = schema.getClass

    while (currentClass != null) {
      val field = currentClass.getDeclaredFields.find(_.getName == fieldName).orNull
      if (field != null) {
        field.setAccessible(true)
        field.set(schema, value.asInstanceOf[AnyRef])
        return
      }
      currentClass = currentClass.getSuperclass
    }
  }
}
