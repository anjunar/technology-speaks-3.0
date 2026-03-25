package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.schema.{EntitySchema, Link, Property}

import java.time.LocalDateTime
import java.util.UUID
import java.util

abstract class AbstractEntitySchema[E <: AbstractEntity] extends EntitySchema[E] {

  val id: Property[E, UUID] = property(_.id)
  val links: Property[E, util.List[Link]] = property(_.links)
  val modified: Property[E, LocalDateTime] = property(_.modified)
  val created: Property[E, LocalDateTime] = property(_.created)

}
