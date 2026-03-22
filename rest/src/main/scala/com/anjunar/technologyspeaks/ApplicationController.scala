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
        .withRel("document")
        .build(),
      LinkBuilder.create(classOf[PostsController], "list")
        .withRel("posts")
        .build(),
      LinkBuilder.create(classOf[UsersController], "list")
        .withRel("users")
        .build(),
      LinkBuilder.create(classOf[FollowersController], "list")
        .withRel("followers")
        .build(),
      LinkBuilder.create(classOf[WebAuthnLoginController], "options")
        .withRel("login")
        .build(),
      LinkBuilder.create(classOf[WebAuthnRegisterController], "options")
        .withRel("register")
        .build(),
      LinkBuilder.create(classOf[PasswordLoginController], "login")
        .withRel("login")
        .build(),
      LinkBuilder.create(classOf[PasswordRegisterController], "register")
        .withRel("register")
        .build(),
      LinkBuilder.create(classOf[LogoutController], "logout")
        .withRel("logout")
        .build(),
      LinkBuilder.create(classOf[ConfirmController], "confirm")
        .withRel("confirm")
        .build()
    )

    application
  }

}
