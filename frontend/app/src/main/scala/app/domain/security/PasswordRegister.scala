package app.domain.security

import app.domain.core.UserInfo
import app.support.{Api, JsonModel, JsonResponse}
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import com.anjunar.scala.enterprise.macros.validation.{EmailConstraint, NotBlank, Size}
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

  override def properties: Seq[PropertyAccess[PasswordRegister, ?]] = PasswordRegister.properties

  def save(): Future[JsonResponse] =
    Api.post("/service/security/register", this)
}

object PasswordRegister {
  val properties: Seq[PropertyAccess[PasswordRegister, ?]] = PropertyMacros.describeProperties[PasswordRegister]
}
