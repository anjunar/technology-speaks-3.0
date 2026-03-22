package com.anjunar.technologyspeaks.core

import com.anjunar.technologyspeaks.hibernate.{EntityContext, RepositoryContext}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.{Column, Entity, Table}
import jakarta.validation.constraints.{NotBlank, Size}

import scala.beans.BeanProperty

@Entity
@Table(name = "Core#Role")
class Role(
  @Size(min = 3, max = 80)
  @NotBlank
  @Column(unique = true)
  @JsonbProperty
  @BeanProperty
  var name: String = null,

  @Size(min = 3, max = 80)
  @NotBlank
  @JsonbProperty
  @BeanProperty
  var description: String = null
) extends AbstractEntity with EntityContext[Role] {
  def this() = this(null, null)
}

object Role extends RepositoryContext[Role]
