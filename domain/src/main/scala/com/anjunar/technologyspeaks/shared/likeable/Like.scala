package com.anjunar.technologyspeaks.shared.likeable

import com.anjunar.technologyspeaks.core.{AbstractEntity, User}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.{Entity, ManyToOne, Table}

import scala.beans.BeanProperty
import scala.compiletime.uninitialized

@Entity
@Table(name = "Shared#Like")
class Like extends AbstractEntity {

  @ManyToOne(optional = false)
  @JsonbProperty
  var user: User = uninitialized

}
