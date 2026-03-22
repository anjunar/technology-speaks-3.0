package com.anjunar.technologyspeaks

import com.anjunar.technologyspeaks.core.UsersController
import com.anjunar.technologyspeaks.documents.{DocumentController, DocumentsController}
import com.anjunar.technologyspeaks.followers.FollowersController
import com.anjunar.technologyspeaks.rest.EntityGraph
import com.anjunar.technologyspeaks.security._
import com.anjunar.technologyspeaks.timeline.PostsController
import jakarta.annotation.security.RolesAllowed
import org.springframework.web.bind.annotation.{GetMapping, RestController}

@RestController
class ApplicationController(val identityHolder: IdentityHolder) {

  @GetMapping(value = Array(""), produces = Array("application/json"))
  @RolesAllowed(Array("Anonymous", "Guest", "User", "Administrator"))
  @EntityGraph("User.full")
  def main(): Application = {
    val application = new Application(identityHolder.user)

    application.addLinks(
      LinkBuilder.create(classOf[DocumentController], "root")
        .withId(true)
        .withRel("document")
        .build(),
      LinkBuilder.create(classOf[PostsController], "list")
        .withId(true)
        .withRel("posts")
        .build(),
      LinkBuilder.create(classOf[UsersController], "list")
        .withId(true)
        .withRel("users")
        .build(),
      LinkBuilder.create(classOf[FollowersController], "list")
        .withId(true)
        .withRel("followers")
        .build(),
      LinkBuilder.create(classOf[WebAuthnLoginController], "options")
        .withId(true)
        .withRel("login")
        .build(),
      LinkBuilder.create(classOf[WebAuthnRegisterController], "options")
        .withId(true)
        .withRel("register")
        .build(),
      LinkBuilder.create(classOf[PasswordLoginController], "login")
        .withId(true)
        .withRel("login")
        .build(),
      LinkBuilder.create(classOf[PasswordRegisterController], "register")
        .withId(true)
        .withRel("register")
        .build(),
      LinkBuilder.create(classOf[LogoutController], "logout")
        .withId(true)
        .withRel("logout")
        .build(),
      LinkBuilder.create(classOf[ConfirmController], "confirm")
        .withId(true)
        .withRel("confirm")
        .build()
    )

    application
  }

}
