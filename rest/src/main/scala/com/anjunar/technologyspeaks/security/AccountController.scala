package com.anjunar.technologyspeaks.security

import com.anjunar.json.mapper.intermediate.model.JsonObject
import com.anjunar.json.mapper.provider.DTO
import com.anjunar.json.mapper.schema.Link
import com.anjunar.technologyspeaks.core.PasswordCredential
import com.anjunar.technologyspeaks.rest.types.Data
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.annotation.security.RolesAllowed
import org.springframework.web.bind.annotation.{GetMapping, PostMapping, RequestBody, RestController}

import java.util
import scala.annotation.meta.field
import scala.jdk.CollectionConverters.*

@RestController
class AccountController(val identityHolder: IdentityHolder) {

  @GetMapping(value = Array("/security/account"), produces = Array("application/json"))
  @RolesAllowed(Array("Guest", "User", "Administrator"))
  def read(): Data[AccountController.AccountResource] = {
    val account = new AccountController.AccountResource()

    account.links.add(
      LinkBuilder.create[AccountController](_.read())
        .withRel("read")
        .build()
    )

    if (currentPasswordCredential() == null) {
      account.links.add(
        LinkBuilder.create[AccountController](_.createPassword(new JsonObject()))
          .withRel("createPassword")
          .build()
      )
    } else {
      account.links.add(
        LinkBuilder.create[AccountController](_.changePassword(new JsonObject()))
          .withRel("changePassword")
          .build()
      )
    }

    new Data(account, null)
  }

  @PostMapping(value = Array("/security/account/password/change"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("Guest", "User", "Administrator"))
  def changePassword(@RequestBody jsonObject: JsonObject): JsonObject = {
    val currentPassword = jsonObject.getString("currentPassword")
    val newPassword = jsonObject.getString("newPassword")
    val confirmPassword = jsonObject.getString("confirmPassword")
    val passwordCredential = currentPasswordCredential()

    if (newPassword == null || newPassword.trim.isEmpty) {
      error("Neues Passwort fehlt.")
    } else if (confirmPassword == null || newPassword != confirmPassword) {
      error("Die neuen Passwoerter stimmen nicht ueberein.")
    } else if (passwordCredential == null) {
      error("Kein Passwort-Zugang fuer diesen Account vorhanden.")
    } else if (currentPassword == null || passwordCredential.password != currentPassword) {
      error("Aktuelles Passwort ist ungueltig.")
    } else {
      passwordCredential.password = newPassword
      success("Passwort aktualisiert.")
    }
  }

  @PostMapping(value = Array("/security/account/password/create"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("Guest", "User", "Administrator"))
  def createPassword(@RequestBody jsonObject: JsonObject): JsonObject = {
    val newPassword = jsonObject.getString("newPassword")
    val confirmPassword = jsonObject.getString("confirmPassword")

    if (newPassword == null || newPassword.trim.isEmpty) {
      error("Neues Passwort fehlt.")
    } else if (confirmPassword == null || newPassword != confirmPassword) {
      error("Die neuen Passwoerter stimmen nicht ueberein.")
    } else if (currentPasswordCredential() != null) {
      error("Fuer diesen Account ist bereits ein Passwort gesetzt.")
    } else {
      val email = firstEmail()

      if (email == null) {
        error("Kein Account-Email-Eintrag gefunden.")
      } else {
        val credential = new PasswordCredential(newPassword, identityHolder.credential.code)
        credential.email = email
        identityHolder.credential.roles.asScala.foreach(credential.roles.add)
        email.credentials.add(credential)
        success("Passwort hinzugefuegt.")
      }
    }
  }

  private def currentPasswordCredential(): PasswordCredential | Null =
    Option(firstEmail())
      .iterator
      .flatMap(email => email.credentials.asScala.iterator)
      .collectFirst { case credential: PasswordCredential => credential }
      .orNull

  private def firstEmail() =
    Option(identityHolder.user)
      .iterator
      .flatMap(user => user.emails.asScala.iterator)
      .find(_ != null)
      .orNull

  private def success(message: String): JsonObject =
    new JsonObject()
      .put("@type", "JsonResponse")
      .put("status", "success")
      .put("message", message)

  private def error(message: String): JsonObject =
    new JsonObject()
      .put("@type", "JsonResponse")
      .put("status", "error")
      .put("message", message)
}

object AccountController {
  class AccountResource extends DTO {
    @(JsonbProperty @field)("@type") val id: String = "Account"
    @(JsonbProperty @field) val links: util.List[Link] = new util.ArrayList[Link]()
  }
}
