package app.domain.security

import app.domain.core.UserInfo
import app.support.{Api, JsonModel, JsonResponse}
import app.support.Api.given
import jfx.core.meta.Meta
import jfx.form.validators.{EmailConstraint, NotBlank, Size}
import jfx.core.state.Property

import scala.concurrent.Future
import scala.scalajs.js

class PasswordRegister(
  @EmailConstraint(message = "Muss eine gueltige E-Mail-Adresse sein")
  val email: Property[String] = Property(""),

  @NotBlank(message = "NickName ist erforderlich")
  @Size(min = 2, max = 80, message = "NickName muss zwischen 2 und 80 Zeichen haben")
  val nickName: Property[String] = Property(""),

  @NotBlank(message = "Passwort ist erforderlich")
  @Size(min = 8, max = 128, message = "Passwort muss zwischen 8 und 128 Zeichen haben")
  val password: Property[String] = Property("")
) extends JsonModel[PasswordRegister] {



  def save(): Future[JsonResponse] =
    Api.post("/service/security/register", this).map(raw => Api.deserialize[JsonResponse](raw))
}

object PasswordRegister {

}
