package app.domain.core

import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import jfx.core.state.{ListProperty, Property}
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import jfx.form.validators.{NotBlankValidator, SizeValidator}

import java.util.UUID
import scala.scalajs.js

class Address extends AbstractEntity[Address] {

  val street: Property[String] = Property("")
  val number: Property[String] = Property("")
  val zipCode: Property[String] = Property("")
  val country: Property[String] = Property("")

  override def properties: Seq[PropertyAccess[Address, ?]] = Address.properties
}

object Address {
  val properties: Seq[PropertyAccess[Address, ?]] = PropertyMacros.describeProperties[Address]
}
