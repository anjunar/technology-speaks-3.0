package app.domain.core

import app.domain.followers.Group
import app.support.Api
import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{ListProperty, Property, PropertyAccess}

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js

class ManagedProperty extends AbstractEntity[ManagedProperty] {

  val name: Property[String] = Property("")
  val visibleForAll: Property[Boolean] = Property(false)
  val users: ListProperty[User] = ListProperty()
  val groups: ListProperty[Group] = ListProperty()

  override def properties: js.Array[PropertyAccess[ManagedProperty, ?]] =
    ManagedProperty.properties

  def updateFromLink(): Future[ManagedProperty] =
    links.find(_.rel == "update") match {
      case Some(link) => Api.invokeLink[ManagedProperty](link, this)
      case None       => Future.successful(this)
    }
}

object ManagedProperty {
  
  val properties: js.Array[PropertyAccess[ManagedProperty, ?]] = js.Array(
    typedProperty[ManagedProperty, Property[UUID], UUID](_.id),
    typedProperty[ManagedProperty, Property[String | Null], String | Null](_.modified),
    typedProperty[ManagedProperty, Property[String | Null], String | Null](_.created),
    typedProperty[ManagedProperty, Property[String], String](_.name),
    typedProperty[ManagedProperty, Property[Boolean], Boolean](_.visibleForAll),
    typedProperty[ManagedProperty, ListProperty[User], User](_.users),
    typedProperty[ManagedProperty, ListProperty[Group], Group](_.groups),
    typedProperty[ManagedProperty, ListProperty[Link], Link](_.links)
  )
}
