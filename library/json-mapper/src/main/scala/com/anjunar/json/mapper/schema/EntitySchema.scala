package com.anjunar.json.mapper.schema


import com.anjunar.json.mapper.macros.{PropertyAccess, PropertyMacros}
import com.anjunar.json.mapper.macros.PropertyMacros.describeProperties
import com.anjunar.json.mapper.schema.property.{ListProperty, Property, SetProperty, SingularProperty}
import com.anjunar.scala.universe.TypeResolver
import jakarta.persistence.EntityManager
import jakarta.persistence.metamodel.{ListAttribute, SetAttribute, SingularAttribute}
import org.hibernate.metamodel.model.domain.PersistentAttribute
import org.hibernate.query.sqm.SqmPathSource

import scala.collection.mutable

abstract class EntitySchema[T](val entityManager: EntityManager = null) {

  val properties: mutable.Map[String, Property[T, Any]] = new mutable.LinkedHashMap[String, Property[T, Any]]()

  def findProperty[V](name: String): Property[T, V] = properties(name).asInstanceOf[Property[T, V]]

  protected inline def property[V](inline selector: T => V,
                                   rule: VisibilityRule[T] = new DefaultRule().asInstanceOf[VisibilityRule[T]]): Property[T, V] = {
    val property = PropertyMacros.makePropertyAccess[T, V](selector)
    val value = new Property[T, V](property, rule)
    properties.put(property.name, value.asInstanceOf[Property[T, Any]])
    value
  }

  protected inline def reference[V](inline selector: T => V,
                                    rule: VisibilityRule[T] = new DefaultRule().asInstanceOf[VisibilityRule[T]]): SingularProperty[T, V] = {
    val property = PropertyMacros.makePropertyAccess[T, V](selector)

    val metamodel = entityManager.getMetamodel
    val entityType = metamodel.entity(TypeResolver.rawType(property.genericType))
    val attribute = entityType.getSingularAttribute(property.name).asInstanceOf[SingularAttribute[T, V] & PersistentAttribute[T, V] & SqmPathSource[V]]
    
    val value = new SingularProperty[T, V](property, rule, attribute)
    properties.put(property.name, value.asInstanceOf[Property[T, Any]])
    value
  }

  protected inline def set[V](inline selector: T => V,
                              rule: VisibilityRule[T] = new DefaultRule().asInstanceOf[VisibilityRule[T]]): SetProperty[T, V] = {
    val property = PropertyMacros.makePropertyAccess[T, V](selector)

    val metamodel = entityManager.getMetamodel
    val entityType = metamodel.entity(Helper.entityType(property.genericType))
    val attribute = entityType.getSet(property.name).asInstanceOf[SetAttribute[T, V]]

    val value = new SetProperty[T, V](property, rule, attribute)
    properties.put(property.name, value.asInstanceOf[Property[T, Any]])
    value
  }

  protected inline def list[V](inline selector: T => V,
                               rule: VisibilityRule[T] = new DefaultRule().asInstanceOf[VisibilityRule[T]]): ListProperty[T, V] = {
    val property = PropertyMacros.makePropertyAccess[T, V](selector)

    val metamodel = entityManager.getMetamodel
    val entityType = metamodel.entity(Helper.entityType(property.genericType))
    val attribute = entityType.getList(property.name).asInstanceOf[ListAttribute[T, V]]

    val value = new ListProperty[T, V](property, rule, attribute)
    properties.put(property.name, value.asInstanceOf[Property[T, Any]])
    value
  }


}
