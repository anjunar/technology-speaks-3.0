package com.anjunar.technologyspeaks.security

import com.anjunar.json.mapper.intermediate.model.JsonObject
import com.anjunar.technologyspeaks.core.{EMail, PasswordCredential, Role, User}
import jakarta.annotation.security.RolesAllowed
import org.springframework.web.bind.annotation.{PostMapping, RequestBody, RestController}

import java.security.SecureRandom

@RestController
class PasswordRegisterController(val registerService: RegisterService, val identityHolder: IdentityHolder) {

  @PostMapping(value = Array("/security/register"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("Anonymous"))
  def register(@RequestBody jsonObject: JsonObject): JsonObject = {
    val nickName = jsonObject.getString("nickName")
    val email = jsonObject.getString("email")
    val password = jsonObject.getString("password")

    var user = User.query("nickName" -> nickName)

    if (user == null) {
      val guestRole = Role.query("name" -> "Guest")

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
      
      identityHolder.postConstruct()
      
      new JsonObject()
        .put("status", "success")
    } else {
      new JsonObject()
        .put("status", "error")
        .put("message", "User already exists")
    }
  }

}
