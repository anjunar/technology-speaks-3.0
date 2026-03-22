package app.domain.security

import app.support.{Api, JsonModel, JsonResponse}
import jfx.core.macros.property
import jfx.core.state.{Property, PropertyAccess}

import scala.concurrent.Future
import scala.scalajs.js

class PasswordLogin(
  var email: Property[String] = Property(""),
  var password: Property[String] = Property("")
) extends JsonModel[PasswordLogin] {

  override def properties: js.Array[PropertyAccess[PasswordLogin, ?]] =
    PasswordLogin.properties

  def save(): Future[JsonResponse] =
    Api.post("/service/security/login", this)
}

object PasswordLogin {
  val properties: js.Array[PropertyAccess[PasswordLogin, ?]] = js.Array(
    property(_.email),
    property(_.password)
  )
}
