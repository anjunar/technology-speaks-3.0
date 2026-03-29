package com.anjunar.technologyspeaks.security

import com.anjunar.json.mapper.intermediate.model.{JsonArray, JsonNode, JsonObject}
import com.anjunar.technologyspeaks.core.Role
import com.anjunar.technologyspeaks.security.WebAuthnManagerProvider.{ORIGIN, RP_ID, RP_NAME, challengeStore, webAuthnManager}
import com.webauthn4j.data.{PublicKeyCredentialParameters, PublicKeyCredentialType, RegistrationParameters}
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier
import com.webauthn4j.data.client.Origin
import com.webauthn4j.data.client.challenge.DefaultChallenge
import com.webauthn4j.server.ServerProperty
import com.webauthn4j.util.Base64UrlUtil
import jakarta.annotation.security.RolesAllowed
import org.springframework.web.bind.annotation.{PostMapping, RequestBody, RestController}

import java.security.SecureRandom
import java.util.Arrays
import scala.jdk.CollectionConverters.*

@RestController
class WebAuthnRegisterController(val store: CredentialStore, val registerService: RegisterService, val sessionHolder: SessionHolder) {

  @PostMapping(value = Array("/security/register/options"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("Anonymous"))
  def options(@RequestBody body: JsonObject): JsonObject = {
    val username = body.getString("email")

    val challengeBytes = new Array[Byte](32)
    new SecureRandom().nextBytes(challengeBytes)
    val challenge = new DefaultChallenge(challengeBytes)
    challengeStore.put(username, challenge)

    val credentials = store.loadByUsername(username)

    val excludeCredentials = new java.util.ArrayList[JsonNode]()
    for (credential <- credentials.asScala) {
      excludeCredentials.add(
        new JsonObject()
          .put("type", "public-key")
          .put("id", store.credentialId(credential))
      )
    }

    new JsonObject()
      .put("challenge", Base64UrlUtil.encodeToString(challengeBytes))
      .put(
        "rp",
        new JsonObject()
          .put("name", RP_NAME)
          .put("id", RP_ID)
      )
      .put(
        "user",
        new JsonObject()
          .put("id", Base64UrlUtil.encodeToString(username.getBytes()))
          .put("name", username)
          .put("displayName", username)
      )
      .put(
        "pubKeyCredParams",
        new JsonArray()
          .add(new JsonObject().put("type", "public-key").put("alg", -7))
          .add(new JsonObject().put("type", "public-key").put("alg", -257))
      )
      .put(
        "authenticatorSelection",
        new JsonObject()
          .put("userVerification", "discouraged")
          .put("requireResidentKey", false)
      )
      .put("attestation", "none")
      .put("timeout", 60000)
      .put("excludeCredentials", new JsonArray(excludeCredentials))
  }

  @PostMapping(value = Array("/security/register/finish"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("Anonymous"))
  def finish(@RequestBody body: JsonObject): JsonObject = {
    val publicKeyCredential = body.getJsonObject("optionsJSON")
    val credentialId = publicKeyCredential.getString("id")
    val username = body.getString("email")
    val nickName = body.getString("nickName")

    val registrationData = webAuthnManager.parseRegistrationResponseJSON(publicKeyCredential.encode())
    val challenge = challengeStore.get(username)

    val serverProperty = new ServerProperty(new Origin(ORIGIN), RP_ID, challenge)
    val pubKeyCredParams = Arrays.asList(
      new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256),
      new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS256)
    )
    val registrationParameters = new RegistrationParameters(
      serverProperty,
      pubKeyCredParams,
      false,
      true
    )

    try {
      webAuthnManager.verify(registrationData, registrationParameters)

      val webAuthnCredentialRecord = new WebAuthnCredentialRecord(
        username,
        registrationData.getAttestationObject,
        registrationData.getCollectedClientData,
        registrationData.getClientExtensions,
        registrationData.getTransports
      )

      val secure = new SecureRandom()
      val n = secure.nextInt(1000000)
      val code = String.format(s"$n%06d", Int.box(n))

      val entity = store.saveRecord(username, nickName, code, webAuthnCredentialRecord)
      registerService.register(username, code, nickName)

      sessionHolder.user = entity.email.user.id
      sessionHolder.credentials = entity.id

      new JsonObject()
        .put("status", "success")
        .put("credentialId", credentialId)
    } catch {
      case ex: Exception =>
        new JsonObject()
          .put("status", "error")
          .put("message", ex.getMessage)
    }
  }

}
