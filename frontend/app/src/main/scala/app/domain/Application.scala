package app.domain

import app.domain.core.{AbstractLink, User, UsersLink}
import app.domain.documents.DocumentsLink
import app.domain.followers.RelationShipLink
import app.domain.security.*
import app.domain.timeline.PostsLink
import app.support.{Api}
import app.support.Api.given
import jfx.core.meta.Meta
import jfx.core.state.ListProperty
import jfx.json.JsonName

import scala.annotation.meta.field
import scala.concurrent.Future

class Application(
  var user: User = new User(),
  @(JsonName @field)("$links")
  val links: ListProperty[AbstractLink] = ListProperty()
)

object Application {

  def read(): Future[Application] = Api.get("/service").map(raw => Api.deserialize[Application](raw))

}
