package app.domain.core

import jfx.core.meta.Meta
import jfx.core.state.Property
import jfx.form.validators.*

class EMail extends AbstractEntity {

  @EmailConstraint(message = "Muss eine gueltige E-Mail-Adresse sein")
  val value: Property[String] = Property("")
  
}