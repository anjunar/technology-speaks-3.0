package com.anjunar.json.mapper.schema.property

import com.anjunar.json.mapper.schema.VisibilityRule
import com.anjunar.json.mapper.schema.jpa.JPAListAttribute
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import jakarta.persistence.metamodel.ListAttribute

import java.lang.reflect.Type

class ListProperty[T,V](propertyAccess: PropertyAccess[T,V], rule: VisibilityRule[T], collectionAttribute: ListAttribute[T,V])
  extends Property[T,V](propertyAccess, rule), JPAListAttribute[T,V](collectionAttribute)