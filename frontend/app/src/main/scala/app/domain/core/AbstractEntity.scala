package app.domain.core

import app.support.JsonModel
import jfx.core.state.{ListProperty, Property}

import java.util.UUID

abstract class AbstractEntity[M] extends JsonModel[M] { self: M =>
  val id: Property[UUID] = Property(null)
  val modified: Property[String] = Property("")
  val created: Property[String] = Property("")
  val links: ListProperty[Link] = ListProperty()
}
