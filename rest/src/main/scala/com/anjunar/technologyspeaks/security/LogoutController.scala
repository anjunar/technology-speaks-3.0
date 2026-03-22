package com.anjunar.technologyspeaks.security

import jakarta.annotation.security.RolesAllowed
import org.springframework.web.bind.annotation.{PostMapping, RestController}

@RestController
class LogoutController(val sessionHolder: SessionHolder) {

  @PostMapping(Array("/security/logout"))
  @RolesAllowed(Array("Guest", "User", "Administrator"))
  def logout(): Unit =
    sessionHolder.invalidate()

}
