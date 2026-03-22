package com.anjunar.technologyspeaks.security

import com.anjunar.json.mapper.intermediate.model.JsonObject
import com.anjunar.technologyspeaks.core.{EMail, PasswordCredential}
import jakarta.annotation.security.RolesAllowed
import org.springframework.web.bind.annotation.{PostMapping, RequestBody, RestController}

import scala.jdk.CollectionConverters._

@RestController
class PasswordLoginController(val sessionHolder: SessionHolder) {

  @PostMapping(value = Array("/security/login"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("Anonymous"))
  def login(@RequestBody jsonObject: JsonObject): JsonObject = {
    val email = jsonObject.getString("email")
    val password = jsonObject.getString("password")

    val eMailEntity = EMail.query("value" -> email)

    if (eMailEntity == null) {
      new JsonObject()
        .put("status", "error")
        .put("message", "User not found")
    } else {
      val credential = eMailEntity.credentials.asScala
        .collect { case value: PasswordCredential => value }
        .find(credential => credential.password == password)
        .orNull

      if (credential != null) {
        sessionHolder.user = eMailEntity.user.id
        sessionHolder.credentials = credential.id

        new JsonObject()
          .put("status", "success")
      } else {
        new JsonObject()
          .put("status", "error")
          .put("message", "Invalid password")
      }
    }
  }

}
