package com.anjunar.technologyspeaks.security

import jakarta.annotation.security.RolesAllowed
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.{PostMapping, RestController}

@RestController
class LogoutController(val sessionHolder: SessionHolder) {

  @PostMapping(Array("/security/logout"))
  @RolesAllowed(Array("Guest", "User", "Administrator"))
  def logout(request: HttpServletRequest): Unit = {
    sessionHolder.invalidate()
    Option(request.getSession(false)).foreach(_.invalidate())
  }

}
