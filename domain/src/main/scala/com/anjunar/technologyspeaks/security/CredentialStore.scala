package com.anjunar.technologyspeaks.security

import com.anjunar.technologyspeaks.core.{EMail, Role, User, WebAuthnCredential}
import com.webauthn4j.credential.CredentialRecord
import org.springframework.stereotype.Service

@Service
class CredentialStore {

  def credentialId(record: CredentialRecord): String =
    record.asInstanceOf[WebAuthnCredentialRecord].getCredentialID

  def loadUser(credentialId: String): User =
    WebAuthnCredential.loadByCredentialId(credentialId).email.user

  def saveRecord(email: String, nickName: String, code: String, record: WebAuthnCredentialRecord): Unit = {
    val roleAction = Role.query("name" -> "Guest")
    val existingEmail = EMail.query("value" -> email)

    val targetEmailFuture =
      if (existingEmail == null) {
        val mail = new EMail(email)
        val user = new User(nickName)

        user.emails.add(mail)
        mail.user = user
        user.persist()
        mail
      } else {
        existingEmail
      }

    val requiredPersistedData = record.getRequiredPersistedData
    val credential = new WebAuthnCredential(
      requiredPersistedData.credentialId(),
      requiredPersistedData.publicKey(),
      requiredPersistedData.publicKeyAlgorithm(),
      requiredPersistedData.counter(),
      requiredPersistedData.aaguid(),
      code
    )

    credential.roles.add(roleAction)
    credential.email = targetEmailFuture
    credential.persist()
  }

  def loadByUsername(username: String): java.util.List[CredentialRecord] =
    WebAuthnCredential.findByEmail(username)
      .stream()
      .map[CredentialRecord](entity =>
        WebAuthnCredentialRecord.fromRequiredPersistedData(
          new WebAuthnCredentialRecord.RequiredPersistedData(
            username,
            entity.credentialId,
            entity.aaguid,
            entity.publicKey,
            entity.publicKeyAlgorithm,
            entity.counter
          )
        )
      )
      .toList()

  def loadByCredentialId(credentialId: String): CredentialRecord = {
    val entity = WebAuthnCredential.loadByCredentialId(credentialId)
    WebAuthnCredentialRecord.fromRequiredPersistedData(
      new WebAuthnCredentialRecord.RequiredPersistedData(
        entity.email.value,
        entity.credentialId,
        entity.aaguid,
        entity.publicKey,
        entity.publicKeyAlgorithm,
        entity.counter
      )
    )
  }

}
