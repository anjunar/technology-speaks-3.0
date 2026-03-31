package app.domain.core

import jfx.core.meta.Meta
import com.anjunar.scala.enterprise.macros.validation.EmailConstraint
import jfx.core.state.Property

class Email extends AbstractEntity[Email] {

  @EmailConstraint(message = "Muss eine gueltige E-Mail-Adresse sein")
  val value: Property[String] = Property("")

  override def meta: Meta[Email] = Email.meta
}

object Email {
  val meta: Meta[Email] = Meta(() => new Email())
}
