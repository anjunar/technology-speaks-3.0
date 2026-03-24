package com.anjunar.technologyspeaks.followers

import com.anjunar.json.mapper.provider.OwnerProvider
import com.anjunar.json.mapper.schema.{EntitySchema, SchemaProvider}
import com.anjunar.technologyspeaks.core.{AbstractEntity, AbstractEntitySchema, OwnerRule, User}
import com.anjunar.technologyspeaks.hibernate.{EntityContext, RepositoryContext}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.{Entity, ManyToOne, Table}
import jakarta.validation.constraints.Size

import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.compiletime.uninitialized

@Entity
@Table(name = "Followers#Group")
class Group(@(JsonbProperty @field) @(Size @field)(min = 3, max = 80)var name: String)
  extends AbstractEntity, OwnerProvider, EntityContext[Group] {

  def this() = this(null)

  @ManyToOne(optional = false)
  @JsonbProperty
  var user: User = uninitialized

  override def owner(): User = user

}

object Group extends RepositoryContext[Group] with SchemaProvider {

  override def schema(): EntitySchema[?] = new Schema

  class Schema extends AbstractEntitySchema[Group] {
    @JsonbProperty val name = property[String]("name", classOf[String], new OwnerRule[Group]())
  }

}
