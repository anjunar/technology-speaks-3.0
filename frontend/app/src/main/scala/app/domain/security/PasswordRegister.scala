package app.domain.security

import app.support.{Api, JsonModel, JsonResponse}
import jfx.core.macros.property
import jfx.core.state.{Property, PropertyAccess}

import scala.concurrent.Future
import scala.scalajs.js

class PasswordRegister(
  var email: Property[String] = Property(""),
  var nickName: Property[String] = Property(""),
  var password: Property[String] = Property("")
) extends JsonModel[PasswordRegister] {

  override def properties: js.Array[PropertyAccess[PasswordRegister, ?]] =
    PasswordRegister.properties

  def save(): Future[JsonResponse] =
    Api.post("/service/security/register", this)
}

object PasswordRegister {
  val properties: js.Array[PropertyAccess[PasswordRegister, ?]] = js.Array(
    property(_.email),
    property(_.nickName),
    property(_.password)
  )
}
