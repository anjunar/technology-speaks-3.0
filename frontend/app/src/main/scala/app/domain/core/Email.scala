package app.domain.core

import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import jfx.core.state.{ListProperty, Property}
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import jfx.form.validators.{EmailValidator, NotBlankValidator}

import java.util.UUID
import scala.scalajs.js

class Email extends AbstractEntity[Email] {

  val value: Property[String] = Property("")

  override def properties: Seq[PropertyAccess[Email, ?]] = Email.properties
}

object Email {
  val properties: Seq[PropertyAccess[Email, ?]] = PropertyMacros.describeProperties[Email]
}
