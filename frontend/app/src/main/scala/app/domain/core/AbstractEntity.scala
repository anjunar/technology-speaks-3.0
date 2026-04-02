package app.domain.core

import jfx.core.state.{ListProperty, Property}
import jfx.json.{JsonIgnore, JsonName}

import java.util.UUID

abstract class AbstractEntity {
  val id: Property[UUID] = Property(null)
  val modified: Property[String | Null] = Property(null)
  val created: Property[String | Null] = Property(null)
  @JsonName("$links")
  val links: ListProperty[Link] = ListProperty()
}
