package com.anjunar.technologyspeaks.core

import com.anjunar.technologyspeaks.hibernate.EntityContext
import jakarta.persistence.Entity

import scala.beans.BeanProperty

@Entity
class PasswordCredential(
  @BeanProperty var password: String = null,
  code: String = null
) extends Credential(code) with EntityContext[PasswordCredential]
