package com.anjunar.technologyspeaks.core

import com.anjunar.technologyspeaks.hibernate.{EntityContext, RepositoryContext}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.{Column, Entity, Table}
import jakarta.validation.constraints.{NotBlank, Size}

import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.compiletime.uninitialized

@Entity
@Table(name = "Core#Role")
class Role(@(Size @field)(min = 3, max = 80)
           @(NotBlank @field)
           @(Column @field)(unique = true)
           @(JsonbProperty @field)
           var name: String,

           @(Size @field)(min = 3, max = 80)
           @(NotBlank @field)
           @(JsonbProperty @field)
           var description: String)
  extends AbstractEntity with EntityContext[Role] {

  def this() = this(null, null)

}

object Role extends RepositoryContext[Role]
