package app.domain.core

import jfx.core.meta.Meta
import com.anjunar.scala.enterprise.macros.validation.EmailConstraint
import jfx.core.state.Property

class EMail extends AbstractEntity[EMail] {

  @EmailConstraint(message = "Muss eine gueltige E-Mail-Adresse sein")
  val value: Property[String] = Property("")

  override def meta: Meta[EMail] = EMail.meta
}

object EMail {
  val meta: Meta[EMail] = Meta(() => new EMail())
}
