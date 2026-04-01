package app.domain

import app.domain.core.AbstractLink
import app.domain.core.User
import app.support.{Api, JsonModel}
import app.support.Api.given
import jfx.core.meta.Meta
import jfx.core.meta.Meta
import jfx.core.state.ListProperty

import scala.concurrent.Future
import scala.scalajs.js

class Application(
  var user: User = new User(),
  val links: ListProperty[AbstractLink] = ListProperty()
) extends JsonModel[Application] {

  override def meta: Meta[Application] = Application.meta

}

object Application {

  val meta = Meta[Application]()

  def read(): Future[Application] = Api.get("/service").map(raw => Api.deserialize(raw, Application.meta))

}
