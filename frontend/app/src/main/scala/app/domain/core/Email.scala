package app.domain.core

import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import com.anjunar.scala.enterprise.macros.validation.EmailConstraint
import jfx.core.state.Property

class Email extends AbstractEntity[Email] {

  @EmailConstraint(message = "Muss eine gueltige E-Mail-Adresse sein")
  val value: Property[String] = Property("")

  override def properties: Seq[PropertyAccess[Email, ?]] = Email.properties
}

object Email {
  val properties: Seq[PropertyAccess[Email, ?]] = PropertyMacros.describeProperties[Email]
}
