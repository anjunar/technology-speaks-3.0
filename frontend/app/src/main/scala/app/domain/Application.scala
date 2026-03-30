package app.domain

import app.domain.core.AbstractLink
import app.domain.core.User
import app.support.{Api, JsonModel}
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import jfx.core.state.ListProperty

import scala.concurrent.Future
import scala.scalajs.js

class Application(
  var user: User = new User(),
  val links: ListProperty[AbstractLink] = ListProperty()
) extends JsonModel[Application] {

  override def properties: Seq[PropertyAccess[Application, ?]] = Application.properties
}

object Application {
  val properties: Seq[PropertyAccess[Application, ?]]= PropertyMacros.describeProperties[Application]
  def read(): Future[Application] =
    Api.get("/service")
}
