package app.domain.security

import app.support.{Api, JsonModel, JsonResponse}
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import jfx.core.state.Property
import jfx.form.validators.{EmailValidator, NotBlankValidator}

import scala.concurrent.Future
import scala.scalajs.js

class PasswordLogin(
  val email: Property[String] = Property(""),
  val password: Property[String] = Property("")
) extends JsonModel[PasswordLogin] {

  override def properties: Seq[PropertyAccess[PasswordLogin, ?]] = PasswordLogin.properties

  def save(): Future[JsonResponse] =
    Api.post("/service/security/login", this)
}

object PasswordLogin {
  val properties: Seq[PropertyAccess[PasswordLogin, ?]]= PropertyMacros.describeProperties[PasswordLogin]}
