package com.anjunar.json.mapper.schema.property

import com.anjunar.json.mapper.schema.VisibilityRule
import com.anjunar.json.mapper.schema.jpa.{JPAListAttribute, JPASetAttribute}
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import jakarta.persistence.metamodel.{ListAttribute, SetAttribute}

import java.lang.reflect.Type

class SetProperty[T,V](propertyAccess: PropertyAccess[T,V], rule: VisibilityRule[T], collectionAttribute: SetAttribute[T,V])
  extends Property[T,V](propertyAccess, rule), JPASetAttribute[T,V](collectionAttribute)