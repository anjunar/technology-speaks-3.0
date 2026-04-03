package app.domain.core

import app.domain.followers.Group
import app.support.Api
import app.support.Api.given
import jfx.core.meta.Meta
import jfx.core.state.{ListProperty, Property}

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js

class ManagedProperty extends AbstractEntity {

  val name: Property[String] = Property("")
  val visibleForAll: Property[Boolean] = Property(false)
  val users: ListProperty[User] = ListProperty()
  val groups: ListProperty[Group] = ListProperty()
  
  def updateFromLink(): Future[ManagedProperty] =
    links.find(_.rel == "update") match {
      case Some(link) => Api.link(link).invoke(this).read[ManagedProperty]
      case None       => Future.successful(this)
    }
}
