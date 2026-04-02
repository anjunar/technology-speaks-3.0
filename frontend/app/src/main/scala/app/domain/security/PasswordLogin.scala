package app.domain.security

import app.support.{Api, JsonModel, JsonResponse}
import app.support.Api.given
import jfx.core.meta.Meta
import jfx.form.validators.{EmailConstraint, NotBlank}
import jfx.core.state.Property

import scala.concurrent.Future
import scala.scalajs.js

class PasswordLogin(
  @EmailConstraint(message = "Muss eine gueltige E-Mail-Adresse sein")
  val email: Property[String] = Property(""),

  @NotBlank(message = "Passwort ist erforderlich")
  val password: Property[String] = Property("")
) extends JsonModel[PasswordLogin] {



  def save(): Future[JsonResponse] =
    Api.post("/service/security/login", this).map(raw => Api.deserialize[JsonResponse](raw))
}
