package com.anjunar.technologyspeaks.security

import com.anjunar.json.mapper.intermediate.model.{JsonArray, JsonNode, JsonObject, JsonString}
import com.anjunar.technologyspeaks.core.{EMail, PasswordCredential, Role, User}
import jakarta.annotation.security.RolesAllowed
import org.springframework.http.{HttpStatus, ResponseEntity}
import org.springframework.web.bind.annotation.{PostMapping, RequestBody, RestController}

import java.security.SecureRandom

@RestController
class PasswordRegisterController(val registerService: RegisterService, val sessionHolder: SessionHolder) {

  @PostMapping(value = Array("/security/register"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("Anonymous"))
  def register(@RequestBody jsonObject: JsonObject): ResponseEntity[JsonNode] = {
    val nickName = jsonObject.getString("nickName")
    val email = jsonObject.getString("email")
    val password = jsonObject.getString("password")

    var user = User.query("nickName" -> nickName)
    val guestRole = Role.query("name" -> "Guest")

    if (user == null) {

      user = new User(nickName)

      val emailEntity = new EMail(email)
      emailEntity.user = user
      user.emails.add(emailEntity)

      val secure = new SecureRandom()
      val n = secure.nextInt(1000000)
      val code = String.format(s"$n%06d", Int.box(n))

      val passwordCredential = new PasswordCredential(password, code)
      passwordCredential.email = emailEntity
      passwordCredential.roles.add(guestRole)
      emailEntity.credentials.add(passwordCredential)

      user.persist()

      registerService.register(email, code, nickName)

      sessionHolder.user = passwordCredential.email.user.id
      sessionHolder.credentials = passwordCredential.id

      new ResponseEntity(new JsonObject()
        .put("status", "success"), HttpStatus.OK)
    } else {

      val eMail = EMail.query("value" -> email)

      if (eMail == null) {

        return new ResponseEntity(new JsonArray()
          .add(new JsonObject()
            .put("path", new JsonArray().add(new JsonString("email")))
            .put("message", "Email wurde nicht gefunden.")), HttpStatus.BAD_REQUEST)

      } else {

        if (user.emails.contains(eMail)) {

          // New Registration of existing user with same email

        } else {
          return new ResponseEntity(new JsonArray()
            .add(new JsonObject()
              .put("path", new JsonArray().add(new JsonString("email")))
              .put("message", "Email passt nicht zum Nickname.")), HttpStatus.BAD_REQUEST)
        }

      }

      val secure = new SecureRandom()
      val n = secure.nextInt(1000000)
      val code = String.format(s"$n%06d", Int.box(n))

      val passwordCredential = new PasswordCredential(password, code)
      passwordCredential.email = eMail
      passwordCredential.roles.add(guestRole)
      eMail.credentials.add(passwordCredential)

      registerService.register(email, code, nickName)

      sessionHolder.user = passwordCredential.email.user.id
      sessionHolder.credentials = passwordCredential.id

      new ResponseEntity(new JsonObject()
        .put("status", "success"), HttpStatus.OK)

    }
  }

}
