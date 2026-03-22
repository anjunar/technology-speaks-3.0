package com.anjunar.technologyspeaks.core

import com.anjunar.technologyspeaks.hibernate.RepositoryContext
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.{Entity, Inheritance, InheritanceType, ManyToMany, ManyToOne, Table}
import jakarta.validation.constraints.Size

import scala.beans.BeanProperty

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "Core#Credential")
class Credential(@BeanProperty var code: String = null) extends AbstractEntity {

  @ManyToMany
  @Size(min = 1, max = 10)
  @JsonbProperty
  @BeanProperty
  val roles: java.util.Set[Role] = new java.util.HashSet[Role]()

  @ManyToOne(optional = false)
  @BeanProperty
  var email: EMail = null

}

object Credential extends RepositoryContext[Credential]
