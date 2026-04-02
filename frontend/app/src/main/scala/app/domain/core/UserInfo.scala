package app.domain.core

import jfx.core.meta.Meta
import jfx.core.state.Property
import jfx.form.validators.*

class UserInfo extends AbstractEntity {

  @NotBlank(message = "Vorname ist erforderlich")
  @Size(min = 2, max = 80, message = "Vorname muss zwischen 2 und 80 Zeichen haben")
  val firstName: Property[String] = Property("")

  @NotBlank(message = "Nachname ist erforderlich")
  @Size(min = 2, max = 80, message = "Nachname muss zwischen 2 und 80 Zeichen haben")
  val lastName: Property[String] = Property("")

  @NotNull(message = "Geburtsdatum ist erforderlich")
  val birthDate: Property[String] = Property("")

}
