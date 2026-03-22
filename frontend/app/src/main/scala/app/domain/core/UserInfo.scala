package app.domain.core

import jfx.core.macros.property
import jfx.core.state.{ListProperty, Property, PropertyAccess}

import java.util.UUID
import scala.scalajs.js

class UserInfo(
                var id: Property[UUID] = Property(null),
                var modified: Property[String] = Property(""),
                var created: Property[String] = Property(""),
                var firstName: Property[String] = Property(""),
                var lastName: Property[String] = Property(""),
                var birthDate: Property[String] = Property(""),
                var links: ListProperty[Link] = ListProperty()
) extends AbstractEntity[UserInfo] {

  override def properties: js.Array[PropertyAccess[UserInfo, ?]] =
    UserInfo.properties
}

object UserInfo {
  val properties: js.Array[PropertyAccess[UserInfo, ?]] = js.Array(
    property(_.id),
    property(_.modified),
    property(_.created),
    property(_.firstName),
    property(_.lastName),
    property(_.birthDate),
    property(_.links)
  )
}
