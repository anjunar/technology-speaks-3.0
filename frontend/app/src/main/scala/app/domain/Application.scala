package app.domain

import app.domain.core.AbstractLink
import app.domain.core.User
import app.support.{Api, JsonModel}
import jfx.core.macros.property
import jfx.core.state.{ListProperty, PropertyAccess}

import scala.concurrent.Future
import scala.scalajs.js

class Application(
  var user: User = new User(),
  var links: ListProperty[AbstractLink] = ListProperty()
) extends JsonModel[Application] {

  override def properties: js.Array[PropertyAccess[Application, ?]] =
    Application.properties
}

object Application {
  val properties: js.Array[PropertyAccess[Application, ?]] = js.Array(
    property(_.user),
    property(_.links)
  )

  def read(): Future[Application] =
    Api.get("/service")
}
