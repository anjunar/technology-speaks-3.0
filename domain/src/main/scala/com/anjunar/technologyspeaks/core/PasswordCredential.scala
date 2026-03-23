package com.anjunar.technologyspeaks.core

import com.anjunar.technologyspeaks.hibernate.EntityContext
import jakarta.persistence.Entity

import scala.beans.BeanProperty
import scala.compiletime.uninitialized

@Entity
class PasswordCredential(var password: String, code: String) 
  extends Credential(code), EntityContext[PasswordCredential] {
  
  def this() = this(null, null)
  
}
