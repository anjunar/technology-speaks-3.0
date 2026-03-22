package com.anjunar.technologyspeaks.security

import com.anjunar.technologyspeaks.core.Role
import jakarta.annotation.security.RolesAllowed
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.{PostMapping, RequestParam, RestController}
import org.springframework.web.server.ResponseStatusException

@RestController
class ConfirmController(val identityHolder: IdentityHolder) {

  @PostMapping(Array("/security/confirm"))
  @RolesAllowed(Array("Guest"))
  def confirm(@RequestParam("code") code: String): Unit = {
    val userRole = Role.query("name" -> "User")

    if (identityHolder.credential.code == code) {
      identityHolder.credential.roles.clear()
      identityHolder.credential.roles.add(userRole)
    } else {
      throw new ResponseStatusException(
        HttpStatus.FORBIDDEN,
        "Access denied"
      )
    }
  }

}
