package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.provider.{EntityProvider, OwnerProvider}
import com.anjunar.json.mapper.schema.{EntitySchema, SchemaProvider}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.{Entity, OneToOne, Table}
import jakarta.validation.constraints.{NotBlank, Size}
import com.anjunar.technologyspeaks.SpringContext
import com.anjunar.technologyspeaks.core.Address.Schema

import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.compiletime.uninitialized

@Entity
@Table(name = "Core#Address")
class Address(@(NotBlank @field)
              @(Size @field)(min = 2, max = 80)
              @(JsonbProperty @field)
              var street: String,

              @(NotBlank @field)
              @(Size @field)(min = 1, max = 80)
              @(JsonbProperty @field) var number: String,

              @(NotBlank @field)
              @(Size @field)(min = 5, max = 5)
              @(JsonbProperty @field) var zipCode: String,

              @(NotBlank @field)
              @(Size @field)(min = 2, max = 80)
              @(JsonbProperty @field)
              var country: String)
  extends AbstractEntity,  OwnerProvider {

  @OneToOne(optional = false, mappedBy = "address")
  var user: User = uninitialized

  def this() = this(null, null, null, null)

  override def owner(): EntityProvider = user.owner()

}

object Address extends SchemaProvider[Schema] {

  class Schema extends AbstractEntitySchema[Address](SpringContext.entityManager()) {
    @JsonbProperty val street = property(_.street, new ManagedRule[Address]())
    @JsonbProperty val number = property(_.number, new ManagedRule[Address]())
    @JsonbProperty val zipCode = property(_.zipCode, new ManagedRule[Address]())
    @JsonbProperty val country = property(_.country, new ManagedRule[Address]())
  }

}
