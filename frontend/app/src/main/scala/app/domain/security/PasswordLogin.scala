package app.domain.security

import app.support.{Api, JsonModel, JsonResponse}
import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{Property, PropertyAccess}
import jfx.form.validators.{EmailValidator, NotBlankValidator}

import scala.concurrent.Future
import scala.scalajs.js

class PasswordLogin(
  val email: Property[String] = Property(""),
  val password: Property[String] = Property("")
) extends JsonModel[PasswordLogin] {

  override def properties: js.Array[PropertyAccess[PasswordLogin, ?]] =
    PasswordLogin.properties

  def save(): Future[JsonResponse] =
    Api.post("/service/security/login", this)
}

object PasswordLogin {
  val properties: js.Array[PropertyAccess[PasswordLogin, ?]] = js.Array(
    typedProperty[PasswordLogin, Property[String], String](_.email)
      .withValidator(NotBlankValidator())
      .withValidator(EmailValidator()),
    typedProperty[PasswordLogin, Property[String], String](_.password)
  )
}
