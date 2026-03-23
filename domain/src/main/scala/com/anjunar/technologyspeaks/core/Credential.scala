package com.anjunar.technologyspeaks.core

import com.anjunar.technologyspeaks.hibernate.RepositoryContext
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.*
import jakarta.validation.constraints.Size

import scala.beans.BeanProperty
import scala.compiletime.uninitialized

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "Core#Credential")
class Credential(var code: String) extends AbstractEntity {

  @ManyToMany
  @Size(min = 1, max = 10)
  @JsonbProperty
  val roles: java.util.Set[Role] = new java.util.HashSet[Role]()
  
  @ManyToOne(optional = false)
  var email: EMail = uninitialized

  def this() = this(null)

}

object Credential extends RepositoryContext[Credential]
