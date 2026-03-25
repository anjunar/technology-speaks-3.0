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

object UserInfo extends SchemaProvider {

  override def schema(): EntitySchema[?] = new Schema

  class Schema extends EntitySchema[UserInfo] {
    @JsonbProperty val id = property(_.id, new ManagedRule[UserInfo]())
    @JsonbProperty val firstName = property(_.firstName, new ManagedRule[UserInfo]())
    @JsonbProperty val lastName = property(_.lastName, new ManagedRule[UserInfo]())
    @JsonbProperty val birthDate = property(_.birthDate, new ManagedRule[UserInfo]())
  }

}
