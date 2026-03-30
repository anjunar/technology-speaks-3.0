package app.domain.core

import app.domain.followers.Group
import app.support.Api
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import jfx.core.state.{ListProperty, Property}
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js

class ManagedProperty extends AbstractEntity[ManagedProperty] {

  val name: Property[String] = Property("")
  val visibleForAll: Property[Boolean] = Property(false)
  val users: ListProperty[User] = ListProperty()
  val groups: ListProperty[Group] = ListProperty()

  override def properties: Seq[PropertyAccess[ManagedProperty, ?]] = ManagedProperty.properties

  def updateFromLink(): Future[ManagedProperty] =
    links.find(_.rel == "update") match {
      case Some(link) => Api.invokeLink[ManagedProperty](link, this)
      case None       => Future.successful(this)
    }
}

object ManagedProperty {

  val properties: Seq[PropertyAccess[ManagedProperty, ?]] = PropertyMacros.describeProperties[ManagedProperty]
}
