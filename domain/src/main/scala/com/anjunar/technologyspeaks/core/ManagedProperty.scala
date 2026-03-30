package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.schema.SchemaProvider
import com.anjunar.technologyspeaks.SpringContext
import com.anjunar.technologyspeaks.followers.Group
import com.anjunar.technologyspeaks.hibernate.EntityContext
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.{Entity, FetchType, ManyToMany, ManyToOne, Table}

import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.compiletime.uninitialized

@Entity
@Table(name = "Core#ManagedProperty")
class ManagedProperty(var name: String, @(JsonbProperty @field) var visibleForAll: Boolean = false)
  extends AbstractEntity with EntityContext[ManagedProperty] {

  def this() = this(null, false)

  @ManyToMany
  @JsonbProperty  
  val users: java.util.Set[User] = new java.util.HashSet[User]()

  @ManyToMany
  @JsonbProperty
  val groups: java.util.Set[Group] = new java.util.HashSet[Group]()

  @ManyToOne(optional = false)
  var view: EntityView = uninitialized

}