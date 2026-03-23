package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.provider.{EntityProvider, OwnerProvider}
import com.anjunar.json.mapper.schema.{EntitySchema, SchemaProvider}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.{Entity, OneToOne, Table}
import jakarta.validation.constraints.{NotBlank, Size}

import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.compiletime.uninitialized

@Entity
@Table(name = "Core#Address")
class Address(@NotBlank @Size(min = 2, max = 80) @(JsonbProperty @field) var street: String,
              @NotBlank @Size(min = 1, max = 80) @(JsonbProperty @field) var number: String,
              @NotBlank @Size(min = 5, max = 5) @(JsonbProperty @field) var zipCode: String,
              @NotBlank @Size(min = 2, max = 80) @(JsonbProperty @field) var country: String)
  extends AbstractEntity,  OwnerProvider {

  @OneToOne(optional = false, mappedBy = "address")
  var user: User = uninitialized

  def this() = this(null, null, null, null)

  override def owner(): EntityProvider = user.owner()

}

object Address extends SchemaProvider {

  override def schema(): EntitySchema[?] = new Schema

  class Schema extends AbstractEntitySchema[Address] {
    @JsonbProperty val street = property[String]("street", classOf[String], new ManagedRule[Address]())
    @JsonbProperty val number = property[String]("number", classOf[String], new ManagedRule[Address]())
    @JsonbProperty val zipCode = property[String]("zipCode", classOf[String], new ManagedRule[Address]())
    @JsonbProperty val country = property[String]("country", classOf[String], new ManagedRule[Address]())
  }

}
