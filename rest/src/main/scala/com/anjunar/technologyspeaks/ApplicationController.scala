package com.anjunar.technologyspeaks

import com.anjunar.json.mapper.intermediate.model.JsonObject
import com.anjunar.technologyspeaks.core.{UserSearch, UsersController}
import com.anjunar.technologyspeaks.documents.{DocumentController, DocumentsController}
import com.anjunar.technologyspeaks.followers.{FollowersController, RelationShipSearch}
import com.anjunar.technologyspeaks.rest.EntityGraph
import com.anjunar.technologyspeaks.security.*
import com.anjunar.technologyspeaks.timeline.{PostSearch, PostsController}
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
      LinkBuilder.create[DocumentController](_.root())
        .withId(true)
        .withRel("document")
        .build(),
      LinkBuilder.create[PostsController](_.list(new PostSearch()))
        .withId(true)
        .withRel("posts")
        .build(),
      LinkBuilder.create[UsersController](_.list(new UserSearch("")))
        .withId(true)
        .withRel("users")
        .build(),
      LinkBuilder.create[FollowersController](_.list(new RelationShipSearch("", new java.util.ArrayList())))
        .withId(true)
        .withRel("followers")
        .build(),
      LinkBuilder.create[WebAuthnLoginController](_.options(new JsonObject()))
        .withId(true)
        .withRel("login")
        .build(),
      LinkBuilder.create[WebAuthnRegisterController](_.options(new JsonObject()))
        .withId(true)
        .withRel("register")
        .build(),
      LinkBuilder.create[PasswordLoginController](_.login(new JsonObject()))
        .withId(true)
        .withRel("login")
        .build(),
      LinkBuilder.create[PasswordRegisterController](_.register(new JsonObject()))
        .withId(true)
        .withRel("register")
        .build(),
      LinkBuilder.create[LogoutController](_.logout())
        .withId(true)
        .withRel("logout")
        .build(),
      LinkBuilder.create[ConfirmController](_.confirm(""))
        .withId(true)
        .withRel("confirm")
        .build()
    )

    application
  }

}
