package app.domain.core

import app.support.JsonModel
import jfx.core.state.{ListProperty, Property}
import jfx.json.{JsonIgnore, JsonName}

import java.util.UUID

abstract class AbstractEntity[M] extends JsonModel[M] { self: M =>
  val id: Property[UUID] = Property(null)
  val modified: Property[String | Null] = Property(null)
  val created: Property[String | Null] = Property(null)
  @JsonName("$links")
  val links: ListProperty[Link] = ListProperty()
}
