package com.anjunar.technologyspeaks.security

import jakarta.annotation.security.RolesAllowed
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.HandlerInterceptor

@Component
class SecurityInterceptor(val identityHolder: IdentityHolder) extends HandlerInterceptor {

  override def preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean =
    handler match {
      case handlerMethod: HandlerMethod =>
        val rolesAllowed = handlerMethod.getMethodAnnotation(classOf[RolesAllowed])
        if (rolesAllowed == null) {
          true
        } else if (!rolesAllowed.value().exists(role => identityHolder.hasRole(role))) {

          if (identityHolder.hasRole("Guest")) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "Access denied")
          } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
          }

        } else {
          true
        }
      case _ =>
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
    }

}
