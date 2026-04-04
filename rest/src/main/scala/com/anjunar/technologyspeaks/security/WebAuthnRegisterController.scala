package com.anjunar.technologyspeaks.security

import com.anjunar.json.mapper.intermediate.model.{JsonArray, JsonNode, JsonObject, JsonString}
import com.anjunar.technologyspeaks.core.{EMail, User}
import com.anjunar.technologyspeaks.rest.EntityManagerProvider
import com.anjunar.technologyspeaks.security.WebAuthnManagerProvider.{ORIGIN, RP_ID, RP_NAME, challengeStore, webAuthnManager}
import com.webauthn4j.data.{PublicKeyCredentialParameters, PublicKeyCredentialType, RegistrationParameters}
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier
import com.webauthn4j.data.client.Origin
import com.webauthn4j.data.client.challenge.DefaultChallenge
import com.webauthn4j.server.ServerProperty
import com.webauthn4j.util.Base64UrlUtil
import jakarta.annotation.security.RolesAllowed
import jakarta.persistence.EntityManager
import org.springframework.http.{HttpStatus, ResponseEntity}
import org.springframework.web.bind.annotation.{PostMapping, RequestBody, RestController}

import java.security.SecureRandom
import scala.jdk.CollectionConverters.*

@RestController
class WebAuthnRegisterController(
  val store: CredentialStore,
  val registerService: RegisterService,
  val sessionHolder: SessionHolder,
  val entityManager: EntityManager
) extends EntityManagerProvider {

  private val timeoutMillis = 60000
  private val challengeLength = 32
  private val pubKeyCredParams = java.util.List.of(
    new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256),
    new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS256)
  )

  @PostMapping(value = Array("/security/register/options"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("Anonymous"))
  def options(@RequestBody body: JsonObject): ResponseEntity[JsonNode] = {
    val nickName = body.getString("nickName")
    val email = body.getString("email")

    validateRegistrationRequest(nickName, email) match {
      case Some(error) => error
      case None =>
        val challengeBytes = createChallenge(email)
        val excludeCredentials = loadExcludeCredentials(email)

        ResponseEntity.ok(
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
                .put("id", Base64UrlUtil.encodeToString(email.getBytes()))
                .put("name", email)
                .put("displayName", email)
            )
            .put("pubKeyCredParams", new JsonArray(pubKeyCredentialParametersJson))
            .put(
              "authenticatorSelection",
              new JsonObject()
                .put("userVerification", "discouraged")
                .put("requireResidentKey", false)
            )
            .put("attestation", "none")
            .put("timeout", timeoutMillis)
            .put("excludeCredentials", new JsonArray(excludeCredentials))
        )
    }
  }

  @PostMapping(value = Array("/security/register/finish"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("Anonymous"))
  def finish(@RequestBody body: JsonObject): ResponseEntity[JsonNode] = {
    val publicKeyCredential = body.getJsonObject("optionsJSON")
    val credentialId = publicKeyCredential.getString("id")
    val email = body.getString("email")
    val nickName = body.getString("nickName")

    val registrationData = webAuthnManager.parseRegistrationResponseJSON(publicKeyCredential.encode())
    val registrationParameters = new RegistrationParameters(
      ServerProperty.builder()
        .origin(new Origin(ORIGIN))
        .rpId(RP_ID)
        .challenge(challengeStore.get(email))
        .build(),
      pubKeyCredParams,
      false,
      true
    )

    try {
      webAuthnManager.verify(registrationData, registrationParameters)

      val verificationCode = createVerificationCode()
      val credentialRecord = new WebAuthnCredentialRecord(
        email,
        registrationData.getAttestationObject,
        registrationData.getCollectedClientData,
        registrationData.getClientExtensions,
        registrationData.getTransports
      )

      val credential = store.saveRecord(email, nickName, verificationCode, credentialRecord)

      sessionHolder.user = credential.email.user.id
      sessionHolder.credentials = credential.id

      registerService.register(email, verificationCode, nickName)

      ResponseEntity.ok(
        new JsonObject()
          .put("status", "success")
          .put("credentialId", credentialId)
      )
    } catch {
      case ex: Exception =>
        fieldErrorResponse("email", ex.getMessage)
    }
  }

  private def validateRegistrationRequest(nickName: String, email: String): Option[ResponseEntity[JsonNode]] = {
    val user = User.query("nickName" -> nickName)

    if (user == null) {
      None
    } else {
      val existingEmail = EMail.query("value" -> email)

      if (existingEmail == null) {
        Some(fieldErrorResponse("email", "Email wurde nicht gefunden."))
      } else if (!user.emails.contains(existingEmail)) {
        Some(fieldErrorResponse("email", "Email passt nicht zum Nickname."))
      } else {
        None
      }
    }
  }

  private def createChallenge(email: String): Array[Byte] = {
    val challengeBytes = new Array[Byte](challengeLength)
    new SecureRandom().nextBytes(challengeBytes)
    challengeStore.put(email, new DefaultChallenge(challengeBytes))
    challengeBytes
  }

  private def loadExcludeCredentials(email: String): java.util.ArrayList[JsonNode] = {
    val excludeCredentials = new java.util.ArrayList[JsonNode]()

    store.loadByUsername(email).asScala.foreach { credential =>
      excludeCredentials.add(
        new JsonObject()
          .put("type", "public-key")
          .put("id", store.credentialId(credential))
      )
    }

    excludeCredentials
  }

  private def createVerificationCode(): String = {
    val number = new SecureRandom().nextInt(1000000)
    f"$number%06d"
  }

  private def fieldErrorResponse(path: String, message: String): ResponseEntity[JsonNode] =
    new ResponseEntity(
      new JsonArray()
        .add(
          new JsonObject()
            .put("path", new JsonArray().add(new JsonString(path)))
            .put("message", message)
        ),
      HttpStatus.BAD_REQUEST
    )

  private def pubKeyCredentialParametersJson: java.util.ArrayList[JsonNode] = {
    val params = new java.util.ArrayList[JsonNode]()
    params.add(new JsonObject().put("type", "public-key").put("alg", -7))
    params.add(new JsonObject().put("type", "public-key").put("alg", -257))
    params
  }
}
