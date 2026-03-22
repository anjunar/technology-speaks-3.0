package com.anjunar.technologyspeaks.core

import com.anjunar.technologyspeaks.hibernate.EntityContext
import jakarta.persistence.{Entity, ManyToMany, ManyToOne, Table}

import scala.beans.BeanProperty

@Entity
@Table(name = "Core#ManagedProperty")
class ManagedProperty(
  @BeanProperty var name: String = null,
  @BeanProperty var visibleForAll: Boolean = false
) extends AbstractEntity with EntityContext[ManagedProperty] {

  @ManyToMany
  @BeanProperty
  val users: java.util.Set[User] = new java.util.HashSet[User]()

  @ManyToOne(optional = false)
  @BeanProperty
  var view: EntityView = null

}
