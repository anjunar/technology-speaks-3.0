package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.provider.{DTO, EntityProvider}
import com.anjunar.technologyspeaks.rest.types.LinksContainer
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.{Id, MappedSuperclass, PrePersist, PreUpdate, Version}

import java.time.LocalDateTime
import java.util.UUID
import scala.beans.BeanProperty
import scala.compiletime.uninitialized

@MappedSuperclass
abstract class AbstractEntity extends EntityProvider with DTO with LinksContainer {

  @Id
  @JsonbProperty
  override val id: UUID = UUID.randomUUID()

  @Version
  @JsonbProperty
  var version: Long = -1L

  @JsonbProperty
  var created: LocalDateTime = uninitialized

  @JsonbProperty
  var modified: LocalDateTime = uninitialized

  @PreUpdate
  def preUpdate(): Unit = {
    modified = LocalDateTime.now()
  }

  @PrePersist
  def prePersist(): Unit = {
    created = LocalDateTime.now()
    modified = LocalDateTime.now()
  }

  def isNew(): Boolean = version == -1L

}
