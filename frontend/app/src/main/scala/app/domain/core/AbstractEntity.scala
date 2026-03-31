package app.domain.core

import com.anjunar.scala.enterprise.macros.validation.JsonName
import app.support.JsonModel
import jfx.core.state.{ListProperty, Property}

import java.util.UUID

abstract class AbstractEntity[M] extends JsonModel[M] { self: M =>
  val id: Property[UUID] = Property(null)
  val modified: Property[String | Null] = Property(null)
  val created: Property[String | Null] = Property(null)
  @JsonName("$links")
  val links: ListProperty[Link] = ListProperty()
}
