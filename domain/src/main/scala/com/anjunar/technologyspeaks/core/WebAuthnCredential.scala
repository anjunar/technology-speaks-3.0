package com.anjunar.technologyspeaks.core

import com.anjunar.technologyspeaks.SpringContext
import com.anjunar.technologyspeaks.hibernate.{EntityContext, RepositoryContext}
import jakarta.persistence.{Entity, NoResultException}

import java.util.UUID
import scala.beans.BeanProperty
import scala.compiletime.uninitialized

@Entity
class WebAuthnCredential(var credentialId: String,
                         var publicKey: Array[Byte] = null,
                         var publicKeyAlgorithm: Long = 0L,
                         var counter: Long = 0L,
                         var aaguid: UUID,
                         code: String = null)
  extends Credential(code), EntityContext[WebAuthnCredential] {

  def this() = this(null, null, 0L, 0L, null, null)

}

object WebAuthnCredential extends RepositoryContext[WebAuthnCredential] {

  def loadByCredentialId(credentialId: String): WebAuthnCredential = {
    val entityManager = SpringContext.entityManager()

    try {
      entityManager
        .createQuery("from WebAuthnCredential c where c.credentialId = :credentialId", classOf[WebAuthnCredential])
        .setParameter("credentialId", credentialId)
        .getSingleResult
    } catch {
      case _: NoResultException => null
    }
  }

  def findByEmail(email: String): java.util.List[WebAuthnCredential] = {
    val entityManager = SpringContext.entityManager()

    entityManager
      .createQuery(
        "select c from WebAuthnCredential c join c.email e where e.value = :email and type(c) = WebAuthnCredential",
        classOf[WebAuthnCredential]
      )
      .setParameter("email", email)
      .getResultList
  }

}
