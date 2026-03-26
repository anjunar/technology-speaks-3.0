package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.provider.{EntityProvider, OwnerProvider}
import com.anjunar.json.mapper.schema.{EntitySchema, SchemaProvider}
import com.anjunar.technologyspeaks.hibernate.{EntityContext, RepositoryContext}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.*
import jakarta.validation.constraints.{Email, NotBlank}

import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.compiletime.uninitialized
import com.anjunar.technologyspeaks.SpringContext

@Entity
@Table(name = "Core#Email")
class EMail(@(JsonbProperty @field) @Email @(NotBlank @field) @Column(unique = true) var value: String)
  extends AbstractEntity, OwnerProvider, EntityContext[EMail] {

  def this() = this(null)

  @ManyToOne(optional = false)
  var user: User = uninitialized

  @OneToMany(cascade = Array(CascadeType.ALL), orphanRemoval = true, mappedBy = "email")
  val credentials: java.util.Set[Credential] = new java.util.HashSet[Credential]()
  
  override def owner(): EntityProvider = user.owner()

}

object EMail extends RepositoryContext[EMail] with SchemaProvider {

  override def schema(): EntitySchema[?] = new Schema

  class Schema extends AbstractEntitySchema[EMail](SpringContext.entityManager()) {
    @JsonbProperty val value = property(_.value, new OwnerRule[EMail]())
  }

}
