package app.domain.core

import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{ListProperty, Property, PropertyAccess}

import java.util.UUID
import scala.scalajs.js

class UserInfo(
                val id: Property[UUID] = Property(null),
                val modified: Property[String] = Property(""),
                val created: Property[String] = Property(""),
                val firstName: Property[String] = Property(""),
                val lastName: Property[String] = Property(""),
                val birthDate: Property[String] = Property(""),
                val links: ListProperty[Link] = ListProperty()
) extends AbstractEntity[UserInfo] {

  override def properties: js.Array[PropertyAccess[UserInfo, ?]] =
    UserInfo.properties
}

object UserInfo {
  val properties: js.Array[PropertyAccess[UserInfo, ?]] = js.Array(
    typedProperty[UserInfo, Property[UUID], UUID](_.id),
    typedProperty[UserInfo, Property[String], String](_.modified),
    typedProperty[UserInfo, Property[String], String](_.created),
    typedProperty[UserInfo, Property[String], String](_.firstName),
    typedProperty[UserInfo, Property[String], String](_.lastName),
    typedProperty[UserInfo, Property[String], String](_.birthDate),
    typedProperty[UserInfo, ListProperty[Link], Link](_.links)
  )
}
