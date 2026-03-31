package app.domain.core

import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import com.anjunar.scala.enterprise.macros.validation.{NotBlank, NotNull, Size}
import jfx.core.state.{ListProperty, Property}
import jfx.form.editor.plugins.Dimensions

import java.util.UUID
import scala.scalajs.js

class UserInfo extends AbstractEntity[UserInfo] {

  @NotBlank(message = "Vorname ist erforderlich")
  @Size(min = 2, max = 80, message = "Vorname muss zwischen 2 und 80 Zeichen haben")
  val firstName: Property[String] = Property("")

  @NotBlank(message = "Nachname ist erforderlich")
  @Size(min = 2, max = 80, message = "Nachname muss zwischen 2 und 80 Zeichen haben")
  val lastName: Property[String] = Property("")

  @NotNull(message = "Geburtsdatum ist erforderlich")
  val birthDate: Property[String] = Property("")

  override def properties: Seq[PropertyAccess[UserInfo, ?]] = UserInfo.properties
}

object UserInfo {
  val properties: Seq[PropertyAccess[UserInfo, ?]] = PropertyMacros.describeProperties[UserInfo]
}
