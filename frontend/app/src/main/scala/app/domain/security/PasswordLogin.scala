package app.domain.security

import app.support.{Api, JsonResponse}
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
) {

  def save(): Future[JsonResponse] =
    Api.request("/service/security/login").post(this).read[JsonResponse]
}
