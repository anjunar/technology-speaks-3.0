package app.domain.security

import app.support.{Api, JsonModel, JsonResponse}
import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{Property, PropertyAccess}
import jfx.form.validators.{EmailValidator, NotBlankValidator, SizeValidator}

import scala.concurrent.Future
import scala.scalajs.js

class PasswordRegister(
  val email: Property[String] = Property(""),
  val nickName: Property[String] = Property(""),
  val password: Property[String] = Property("")
) extends JsonModel[PasswordRegister] {

  override def properties: js.Array[PropertyAccess[PasswordRegister, ?]] =
    PasswordRegister.properties

  def save(): Future[JsonResponse] =
    Api.post("/service/security/register", this)
}

object PasswordRegister {
  val properties: js.Array[PropertyAccess[PasswordRegister, ?]] = js.Array(
    typedProperty[PasswordRegister, Property[String], String](_.email)
      .withValidator(NotBlankValidator())
      .withValidator(EmailValidator()),
    typedProperty[PasswordRegister, Property[String], String](_.nickName)
      .withValidator(NotBlankValidator())
      .withValidator(SizeValidator(2, 80)),
    typedProperty[PasswordRegister, Property[String], String](_.password)
  )
}
