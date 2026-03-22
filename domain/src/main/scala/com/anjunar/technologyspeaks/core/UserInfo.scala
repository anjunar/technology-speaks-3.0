package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.provider.{EntityProvider, OwnerProvider}
import com.anjunar.json.mapper.schema.{EntitySchema, SchemaProvider}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.{Entity, OneToOne, Table}
import jakarta.validation.constraints.{NotBlank, NotNull, Size}

import java.time.LocalDate
import scala.annotation.meta.field
import scala.beans.BeanProperty

@Entity
@Table(name = "Core#UserInfo")
class UserInfo(
  @NotBlank @Size(min = 2, max = 80) @(JsonbProperty @field) @BeanProperty var firstName: String = null,
  @NotBlank @Size(min = 2, max = 80) @(JsonbProperty @field) @BeanProperty var lastName: String = null,
  @NotNull @(JsonbProperty @field) @BeanProperty var birthDate: LocalDate = null
) extends AbstractEntity with OwnerProvider {
  
  def this() = this(null, null, null)

  @OneToOne(optional = false, mappedBy = "info")
  @BeanProperty
  var user: User = null

  override def owner(): EntityProvider = user.owner()

}

object UserInfo extends SchemaProvider {

  override def schema(): EntitySchema[?] = new Schema

  class Schema extends EntitySchema[UserInfo] {
    @JsonbProperty val id = property[java.util.UUID]("id", classOf[java.util.UUID], new ManagedRule[UserInfo]())
    @JsonbProperty val firstName = property[String]("firstName", classOf[String], new ManagedRule[UserInfo]())
    @JsonbProperty val lastName = property[String]("lastName", classOf[String], new ManagedRule[UserInfo]())
    @JsonbProperty val birthDate = property[LocalDate]("birthDate", classOf[LocalDate], new ManagedRule[UserInfo]())
  }

}
