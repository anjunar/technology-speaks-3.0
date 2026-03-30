package app.domain.security

import app.domain.core.UserInfo
import app.support.{Api, JsonModel, JsonResponse}
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import jfx.core.state.Property
import jfx.form.validators.{EmailValidator, NotBlankValidator, SizeValidator}

import scala.concurrent.Future
import scala.scalajs.js

class PasswordRegister(
  val email: Property[String] = Property(""),
  val nickName: Property[String] = Property(""),
  val password: Property[String] = Property("")
) extends JsonModel[PasswordRegister] {

  override def properties: Seq[PropertyAccess[PasswordRegister, ?]] = PasswordRegister.properties

  def save(): Future[JsonResponse] =
    Api.post("/service/security/register", this)
}

object PasswordRegister {
  val properties: Seq[PropertyAccess[PasswordRegister, ?]] = PropertyMacros.describeProperties[PasswordRegister]
}
