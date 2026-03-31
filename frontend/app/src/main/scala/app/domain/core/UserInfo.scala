package app.domain.core

import jfx.core.meta.Meta
import com.anjunar.scala.enterprise.macros.validation.{NotBlank, NotNull, Size}
import jfx.core.state.Property

class UserInfo extends AbstractEntity[UserInfo] {

  @NotBlank(message = "Vorname ist erforderlich")
  @Size(min = 2, max = 80, message = "Vorname muss zwischen 2 und 80 Zeichen haben")
  val firstName: Property[String] = Property("")

  @NotBlank(message = "Nachname ist erforderlich")
  @Size(min = 2, max = 80, message = "Nachname muss zwischen 2 und 80 Zeichen haben")
  val lastName: Property[String] = Property("")

  @NotNull(message = "Geburtsdatum ist erforderlich")
  val birthDate: Property[String] = Property("")

  override def meta: Meta[UserInfo] = UserInfo.meta
}

object UserInfo {
  val meta: Meta[UserInfo] = Meta(() => new UserInfo())
}
