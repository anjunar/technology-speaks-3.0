package app.domain.core

import jfx.core.meta.Meta
import com.anjunar.scala.enterprise.macros.validation.{NotBlank, Size}
import jfx.core.state.Property

class Address extends AbstractEntity[Address] {

  @NotBlank(message = "Straße ist erforderlich")
  @Size(min = 2, max = 80, message = "Straße muss zwischen 2 und 80 Zeichen haben")
  val street: Property[String] = Property("")

  @NotBlank(message = "Hausnummer ist erforderlich")
  @Size(min = 1, max = 80, message = "Hausnummer muss zwischen 1 und 80 Zeichen haben")
  val number: Property[String] = Property("")

  @NotBlank(message = "PLZ ist erforderlich")
  @Size(min = 5, max = 5, message = "PLZ muss genau 5 Zeichen haben")
  val zipCode: Property[String] = Property("")

  @NotBlank(message = "Land ist erforderlich")
  @Size(min = 2, max = 80, message = "Land muss zwischen 2 und 80 Zeichen haben")
  val country: Property[String] = Property("")

  override def meta: Meta[Address] = Address.meta
}

object Address {
  val meta: Meta[Address] = Meta(() => new Address())
}
