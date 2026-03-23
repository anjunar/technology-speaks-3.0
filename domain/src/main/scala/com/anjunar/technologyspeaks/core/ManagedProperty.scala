package com.anjunar.technologyspeaks.core

import com.anjunar.technologyspeaks.hibernate.EntityContext
import jakarta.persistence.{Entity, ManyToMany, ManyToOne, Table}

import scala.beans.BeanProperty
import scala.compiletime.uninitialized

@Entity
@Table(name = "Core#ManagedProperty")
class ManagedProperty(var name: String, var visibleForAll: Boolean = false)
  extends AbstractEntity with EntityContext[ManagedProperty] {

  def this() = this(null, false)

  @ManyToMany
  val users: java.util.Set[User] = new java.util.HashSet[User]()

  @ManyToOne(optional = false)
  var view: EntityView = uninitialized

}
