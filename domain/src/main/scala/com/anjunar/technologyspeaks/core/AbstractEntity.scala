package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.provider.{DTO, EntityProvider}
import com.anjunar.technologyspeaks.rest.types.LinksContainer
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.{Id, MappedSuperclass, PrePersist, PreUpdate, Version}

import java.time.LocalDateTime
import java.util.UUID
import scala.beans.BeanProperty

@MappedSuperclass
abstract class AbstractEntity extends EntityProvider with DTO with LinksContainer.Trait {

  @Id
  @JsonbProperty
  @BeanProperty
  override val id: UUID = UUID.randomUUID()

  @Version
  @JsonbProperty
  @BeanProperty
  var version: Long = -1L

  @JsonbProperty
  @BeanProperty
  var created: LocalDateTime = null

  @JsonbProperty
  @BeanProperty
  var modified: LocalDateTime = null

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
