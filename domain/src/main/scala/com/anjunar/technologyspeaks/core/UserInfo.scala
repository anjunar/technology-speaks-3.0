package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.provider.{EntityProvider, OwnerProvider}
import com.anjunar.json.mapper.schema.{EntitySchema, SchemaProvider}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.{Entity, OneToOne, Table}
import jakarta.validation.constraints.{NotBlank, NotNull, Size}

import java.time.LocalDate
import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.compiletime.uninitialized
import com.anjunar.technologyspeaks.SpringContext
import com.anjunar.technologyspeaks.core.UserInfo.Schema

@Entity
@Table(name = "Core#UserInfo")
class UserInfo(@(NotBlank @field) 
               @(Size @field)(min = 2, max = 80) 
               @(JsonbProperty @field) 
               var firstName: String,
              
               @(NotBlank @field) 
               @(Size @field)(min = 2, max = 80)
               @(JsonbProperty @field) var lastName: String,
              
               @(NotNull @field) 
               @(JsonbProperty @field) 
               var birthDate: LocalDate)
  extends AbstractEntity, OwnerProvider {

  def this() = this(null, null, null)

  @OneToOne(optional = false, mappedBy = "info")
  var user: User = uninitialized

  override def owner(): EntityProvider = user.owner()

}

object UserInfo extends SchemaProvider[Schema] {

  class Schema extends EntitySchema[UserInfo](SpringContext.entityManager()) {
    @JsonbProperty val id = property(_.id, classOf[ManagedRule[UserInfo]])
    @JsonbProperty val firstName = property(_.firstName, classOf[ManagedRule[UserInfo]])
    @JsonbProperty val lastName = property(_.lastName, classOf[ManagedRule[UserInfo]])
    @JsonbProperty val birthDate = property(_.birthDate, classOf[ManagedRule[UserInfo]])
  }

}
