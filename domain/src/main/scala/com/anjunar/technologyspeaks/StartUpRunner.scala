package com.anjunar.technologyspeaks

import com.anjunar.technologyspeaks.core.Role
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class StartUpRunner {

  @Transactional
  def run(args: String*): Unit = {
    var anonymousRole = Role.query("name" -> "Anonymous")
    var guestRole = Role.query("name" -> "Guest")
    var userRole = Role.query("name" -> "User")
    var administratorRole = Role.query("name" -> "Administrator")

    if (anonymousRole == null) {
      anonymousRole = new Role("Anonymous", "Anonymous User")
      anonymousRole.persist()
    }

    if (guestRole == null) {
      guestRole = new Role("Guest", "Guest User")
      guestRole.persist()
    }

    if (userRole == null) {
      userRole = new Role("User", "User User")
      userRole.persist()
    }

    if (administratorRole == null) {
      administratorRole = new Role("Administrator", "Administrator User")
      administratorRole.persist()
    }
  }

}
