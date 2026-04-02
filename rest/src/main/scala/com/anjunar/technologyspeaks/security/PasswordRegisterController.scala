package com.anjunar.technologyspeaks.security

import com.anjunar.json.mapper.intermediate.model.{JsonArray, JsonNode, JsonObject, JsonString}
import com.anjunar.technologyspeaks.core.{EMail, PasswordCredential, Role, User}
import com.anjunar.technologyspeaks.rest.EntityManagerProvider
import jakarta.annotation.security.RolesAllowed
import jakarta.persistence.EntityManager
import org.springframework.http.{HttpStatus, ResponseEntity}
import org.springframework.web.bind.annotation.{PostMapping, RequestBody, RestController}

import java.security.SecureRandom
import scala.jdk.CollectionConverters.*

@RestController
class PasswordRegisterController(val registerService: RegisterService, val sessionHolder: SessionHolder, val entityManager : EntityManager) extends EntityManagerProvider {

  @PostMapping(value = Array("/security/register"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("Anonymous"))
  def register(@RequestBody jsonObject: PasswordRegistration): ResponseEntity[JsonNode] = {
    val nickName = jsonObject.nickName
    val email = jsonObject.email
    val password = jsonObject.password

    val user = User.query("nickName" -> nickName)
    val existingEmail = EMail.query("value" -> email)

    if (user == null && existingEmail != null) {
      return validationError("email", "Email ist bereits vergeben.")
    }

    if (user != null && existingEmail == null) {
      return validationError("email", "Email wurde nicht gefunden.")
    }

    if (user != null && existingEmail.user != user) {
      return validationError("email", "Email passt nicht zum Nickname.")
    }

    val guestRole = Role.query("name" -> "Guest")
    val verificationCode = generateCode()

    val resolvedUser =
      if (user != null) user
      else new User(nickName)

    val resolvedEmail =
      if (existingEmail != null) existingEmail
      else {
        val emailEntity = new EMail(email)
        emailEntity.user = resolvedUser
        resolvedUser.emails.add(emailEntity)
        emailEntity
      }

    val credential = upsertPasswordCredential(resolvedEmail, password, verificationCode, guestRole)

    if (resolvedUser.id == null) {
      resolvedUser.persist()
    } else if (credential.id == null) {
      credential.persist()
    }

    registerService.register(resolvedEmail.value, verificationCode, resolvedUser.nickName)

    sessionHolder.user = resolvedUser.id
    sessionHolder.credentials = credential.id

    success()
  }

  private def upsertPasswordCredential(email: EMail, password: String, verificationCode: String, guestRole: Role): PasswordCredential = {
    val existingCredential = email.credentials.asScala.collectFirst { case credential: PasswordCredential => credential }.orNull

    if (existingCredential != null) {
      existingCredential.password = password
      existingCredential.code = verificationCode
      existingCredential.roles.add(guestRole)
      existingCredential
    } else {
      val credential = new PasswordCredential(password, verificationCode)
      credential.email = email
      credential.roles.add(guestRole)
      email.credentials.add(credential)
      credential
    }
  }

  private def generateCode(): String = {
    val n = new SecureRandom().nextInt(1000000)
    String.format(s"$n%06d", Int.box(n))
  }

  private def success(): ResponseEntity[JsonNode] =
    new ResponseEntity(
      new JsonObject()
        .put("status", "success")
        .put("message", ""),
      HttpStatus.OK
    )

  private def validationError(path: String, message: String): ResponseEntity[JsonNode] =
    new ResponseEntity(
      new JsonArray()
        .add(
          new JsonObject()
            .put("path", new JsonArray().add(new JsonString(path)))
            .put("message", message)
        ),
      HttpStatus.BAD_REQUEST
    )
}
