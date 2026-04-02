package com.anjunar.technologyspeaks.security

import com.anjunar.technologyspeaks.core.{Credential, PasswordCredential, User}
import com.anjunar.technologyspeaks.rest.EntityManagerProvider
import com.typesafe.scalalogging.Logger
import jakarta.annotation.PostConstruct
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

import scala.compiletime.uninitialized
import java.util

@Component
@RequestScope
class IdentityHolder(val sessionHolder: SessionHolder, val entityManager: EntityManager) extends EntityManagerProvider {

  val log = Logger[IdentityHolder]
  
  var user: User = uninitialized

  var roles: java.util.List[String] = null

  var credential: Credential = uninitialized

  def isAuthenticated: Boolean = sessionHolder.user != null

  def hasRole(role: String): Boolean = roles.contains(role)

  @PostConstruct
  def postConstruct(): Unit = {
    if (sessionHolder.user == null || sessionHolder.credentials == null) {
      log.debug("Anonymous user")
      user = new User("Anonymous")
      roles = util.List.of("Anonymous")
      credential = new PasswordCredential("Anonymous", "Anonymous")
    } else {
      user = User.find(sessionHolder.user)
      credential = entityManager.find(classOf[Credential], sessionHolder.credentials)
      roles = credential.roles.stream().map[String](role => role.name).toList
    }
  }

}
