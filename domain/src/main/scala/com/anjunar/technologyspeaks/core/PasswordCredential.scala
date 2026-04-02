package com.anjunar.technologyspeaks.core

import com.anjunar.technologyspeaks.hibernate.EntityContext
import jakarta.persistence.Entity
import jakarta.validation.constraints.{NotBlank, Size}

import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.compiletime.uninitialized

@Entity
class PasswordCredential(@(NotBlank @field)
                         @(Size @field)(min = 4, max = 128)
                         var password: String, code: String)
  extends Credential(code), EntityContext[PasswordCredential] {

  def this() = this(null, null)

}
