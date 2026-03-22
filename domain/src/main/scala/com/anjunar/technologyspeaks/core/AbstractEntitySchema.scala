package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.schema.{EntitySchema, Link}
import java.time.LocalDateTime
import java.util.UUID

abstract class AbstractEntitySchema[E <: AbstractEntity] extends EntitySchema[E] {

  val id = property[UUID]("id", classOf[UUID])
  val links = property[java.util.List[Link]]("links", classOf[java.util.List[?]], collectionType = classOf[Link])
  val modified = property[LocalDateTime]("modified", classOf[LocalDateTime])
  val created = property[LocalDateTime]("created", classOf[LocalDateTime])

}
