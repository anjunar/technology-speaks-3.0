package com.anjunar.technologyspeaks.security

import com.anjunar.json.mapper.intermediate.model.{JsonArray, JsonNode, JsonObject}
import com.anjunar.technologyspeaks.core.WebAuthnCredential
import com.anjunar.technologyspeaks.security.WebAuthnManagerProvider.{ORIGIN, RP_ID, challengeStore, webAuthnManager}
import com.webauthn4j.data.AuthenticationParameters
import com.webauthn4j.data.client.Origin
import com.webauthn4j.data.client.challenge.DefaultChallenge
import com.webauthn4j.server.ServerProperty
import com.webauthn4j.util.Base64UrlUtil
import com.webauthn4j.verifier.exception.VerificationException
import jakarta.annotation.security.RolesAllowed
import jakarta.persistence.{EntityManager, NoResultException}
import org.springframework.web.bind.annotation.{PostMapping, RequestBody, RestController}

import java.security.SecureRandom
import scala.jdk.CollectionConverters._

@RestController
class WebAuthnLoginController(val store: CredentialStore, val entityManager: EntityManager, val sessionHolder: SessionHolder) {

  @PostMapping(value = Array("/security/login/options"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("Anonymous"))
  def options(@RequestBody request: JsonObject): JsonObject = {
    val username = request.getString("email")

    val challengeBytes = new Array[Byte](32)
    new SecureRandom().nextBytes(challengeBytes)
    val challenge = new DefaultChallenge(challengeBytes)
    challengeStore.put(username, challenge)

    val credentials = store.loadByUsername(username)

    val allowCredentials = new java.util.ArrayList[JsonNode]()
    for (credential <- credentials.asScala) {
      allowCredentials.add(
        new JsonObject()
          .put("type", "public-key")
          .put("id", store.credentialId(credential))
      )
    }

    new JsonObject()
      .put("challenge", Base64UrlUtil.encodeToString(challengeBytes))
      .put("rpId", RP_ID)
      .put("allowCredentials", new JsonArray(allowCredentials))
      .put("userVerification", "discouraged")
      .put("timeout", 60000)
  }

  @PostMapping(value = Array("/security/login/finish"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("Anonymous"))
  def finish(@RequestBody body: JsonObject): JsonObject = {
    val publicKeyCredential = body.getJsonObject("optionsJSON")
    val username = body.getString("email")
    val credentialId = publicKeyCredential.getString("id")
    if (credentialId.isEmpty) {
      throw new IllegalArgumentException("Credential ID is missing in response")
    }

    val authenticationData = webAuthnManager.parseAuthenticationResponseJSON(publicKeyCredential.encode())

    val credential = WebAuthnCredential.loadByCredentialId(credentialId)
    
    if (credential == null) {
      return new JsonObject()
        .put("status", "error")
        .put("message", "Credential not found.")
    }
    
    val credentialRecord = store.loadByCredentialId(credential)

    val challenge = challengeStore.get(username)

    val serverProperty = ServerProperty.builder()
      .origin(new Origin(ORIGIN))
      .rpId(RP_ID)
      .challenge(challenge)
      .build()
    val authenticationParameters = new AuthenticationParameters(
      serverProperty,
      credentialRecord,
      null,
      false,
      true
    )

    try {
      val verifiedAuthenticationData = webAuthnManager.verify(authenticationData, authenticationParameters)

      val entity =
        try {
          entityManager
            .createQuery(
              "from WebAuthnCredential c join fetch c.roles r join fetch c.email e join fetch e.user where c.credentialId = : credentialId",
              classOf[WebAuthnCredential]
            )
            .setParameter("credentialId", credentialId)
            .getSingleResult
        } catch {
          case _: NoResultException => null
        }

      entity.counter = verifiedAuthenticationData.getAuthenticatorData.getSignCount

      val user = store.loadUser(credentialId)

      sessionHolder.user = user.id
      sessionHolder.credentials = entity.id

      new JsonObject()
        .put("status", "success")
        .put("user", user.nickName)
    } catch {
      case ex: VerificationException =>
        new JsonObject()
          .put("status", "error")
          .put("message", ex.getMessage.toString)
    }
  }

}
