package app.domain.core

import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import jfx.core.state.{ListProperty, Property}
import jfx.form.editor.plugins.Dimensions
import jfx.form.validators.{NotBlankValidator, NotNullValidator, SizeValidator}

import java.util.UUID
import scala.scalajs.js

class UserInfo extends AbstractEntity[UserInfo] {

  val firstName: Property[String] = Property("")
  val lastName: Property[String] = Property("")
  val birthDate: Property[String] = Property("")

  override def properties: Seq[PropertyAccess[UserInfo, ?]] = UserInfo.properties
}

object UserInfo {
  val properties: Seq[PropertyAccess[UserInfo, ?]] = PropertyMacros.describeProperties[UserInfo]
}
