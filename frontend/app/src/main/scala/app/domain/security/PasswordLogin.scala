package app.domain.security

import app.support.{Api, JsonModel, JsonResponse}
import jfx.core.meta.Meta
import com.anjunar.scala.enterprise.macros.validation.{EmailConstraint, NotBlank}
import jfx.core.state.Property

import scala.concurrent.Future
import scala.scalajs.js

class PasswordLogin(
  @EmailConstraint(message = "Muss eine gueltige E-Mail-Adresse sein")
  val email: Property[String] = Property(""),

  @NotBlank(message = "Passwort ist erforderlich")
  val password: Property[String] = Property("")
) extends JsonModel[PasswordLogin] {

  override def meta: Meta[PasswordLogin] = PasswordLogin.meta

  def save(): Future[JsonResponse] =
    Api.post("/service/security/login", this)
}

object PasswordLogin {
  val meta : Meta[PasswordLogin] = Meta()}
