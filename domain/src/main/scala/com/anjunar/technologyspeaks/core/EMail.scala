package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.provider.{EntityProvider, OwnerProvider}
import com.anjunar.json.mapper.schema.{EntitySchema, SchemaProvider}
import com.anjunar.technologyspeaks.hibernate.{EntityContext, RepositoryContext}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.{CascadeType, Column, Entity, ManyToOne, OneToMany, Table}
import jakarta.validation.constraints.{Email, NotBlank}

import scala.annotation.meta.field
import scala.beans.BeanProperty

@Entity
@Table(name = "Core#Email")
class EMail(
             @(JsonbProperty @field) @Email @NotBlank @Column(unique = true) @BeanProperty var value: String = null
) extends AbstractEntity with OwnerProvider with EntityContext[EMail] {
  
  def this() = this(null)

  @ManyToOne(optional = false)
  @BeanProperty
  var user: User = null

  @OneToMany(cascade = Array(CascadeType.ALL), orphanRemoval = true, mappedBy = "email")
  @BeanProperty
  val credentials: java.util.Set[Credential] = new java.util.HashSet[Credential]()

  override def owner(): EntityProvider = user.owner()

}

object EMail extends RepositoryContext[EMail] with SchemaProvider {

  override def schema(): EntitySchema[?] = new Schema

  class Schema extends AbstractEntitySchema[EMail] {
    @JsonbProperty val value = property[String]("value", classOf[String], new OwnerRule[EMail]())
  }

}
