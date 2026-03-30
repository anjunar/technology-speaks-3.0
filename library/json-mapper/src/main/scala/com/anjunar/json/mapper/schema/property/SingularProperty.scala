package com.anjunar.json.mapper.schema.property

import com.anjunar.json.mapper.schema.VisibilityRule
import com.anjunar.json.mapper.schema.jpa.JPASingularAttribute
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import jakarta.persistence.metamodel.SingularAttribute
import org.hibernate.metamodel.model.domain.PersistentAttribute
import org.hibernate.query.sqm.SqmPathSource

import java.lang.reflect.Type

class SingularProperty[T,V](propertyAccess: PropertyAccess[T,V], 
                            rule: VisibilityRule[T], 
                            collectionAttribute: SingularAttribute[T,V] & PersistentAttribute[T,V] & SqmPathSource[V]) 
  extends Property[T,V](propertyAccess, rule), JPASingularAttribute[T,V](collectionAttribute)
