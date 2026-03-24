package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.schema.{EntitySchema, Link, Property}

import java.time.LocalDateTime
import java.util.UUID
import java.util

abstract class AbstractEntitySchema[E <: AbstractEntity] extends EntitySchema[E] {

  val id: Property[E, UUID] = property[UUID]("id", classOf[UUID])
  val links: Property[E, util.List[Link]] = property[util.List[Link]]("links", classOf[util.List[?]], collectionType = classOf[Link])
  val modified: Property[E, LocalDateTime] = property[LocalDateTime]("modified", classOf[LocalDateTime])
  val created: Property[E, LocalDateTime] = property[LocalDateTime]("created", classOf[LocalDateTime])

}
