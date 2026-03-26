package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.schema.property.Property
import com.anjunar.json.mapper.schema.{EntitySchema, Link}
import jakarta.persistence.EntityManager

import java.time.LocalDateTime
import java.util.UUID
import java.util

abstract class AbstractEntitySchema[E <: AbstractEntity](entityManager: EntityManager) extends EntitySchema[E](entityManager) {

  val id: Property[E, UUID] = property(_.id)
  val links: Property[E, util.List[Link]] = property(_.links)
  val modified: Property[E, LocalDateTime] = property(_.modified)
  val created: Property[E, LocalDateTime] = property(_.created)

}
