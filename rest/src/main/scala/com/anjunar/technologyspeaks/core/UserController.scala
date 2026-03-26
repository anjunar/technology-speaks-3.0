package com.anjunar.technologyspeaks.core

import com.anjunar.technologyspeaks.followers.{FollowerController, RelationShip, UserGroupsController}
import com.anjunar.technologyspeaks.rest.EntityGraph
import com.anjunar.technologyspeaks.rest.types.Data
import com.anjunar.technologyspeaks.security.{IdentityHolder, LinkBuilder}
import jakarta.annotation.security.RolesAllowed
import org.springframework.web.bind.annotation.{GetMapping, PathVariable, PutMapping, RequestBody, RestController}

@RestController
class UserController(val identityHolder: IdentityHolder) {

  @GetMapping(value = Array("/core/users/user/{id}"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @EntityGraph("User.full")
  def read(@PathVariable("id") user: User): Data[User] = {
    val form = new Data(user, User.schema)

    if (identityHolder.user == user) {
      user.addLinks(
        LinkBuilder.create(classOf[UserController], "update")
          .build()
      )
    }

    val relationShip = RelationShip.query("follower" -> user, "user" -> identityHolder.user)

    user.addLinks(
      if (relationShip == null) {
        LinkBuilder.create(classOf[FollowerController], "follow")
          .withRel("follow")
          .withVariable("id", user.id)
          .build()
      } else {
        LinkBuilder.create(classOf[FollowerController], "unfollow")
          .withRel("unfollow")
          .withVariable("id", user.id)
          .build()
      }
    )

    if (relationShip != null) {
      user.addLinks(
        LinkBuilder.create(classOf[UserGroupsController], "list")
          .withRel("groups")
          .withVariable("id", user.id)
          .build(),
        LinkBuilder.create(classOf[UserGroupsController], "update")
          .withRel("updateGroups")
          .withVariable("id", user.id)
          .build()
      )
    }

    form
  }

  @PutMapping(value = Array("/core/users/user"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @EntityGraph("User.full")
  def update(@RequestBody user: User): Data[User] = {
    val form = new Data(user, User.schema)

    user.addLinks(
      LinkBuilder.create(classOf[UserController], "update")
        .build(),
      LinkBuilder.create(classOf[ManagedPropertyController], "read")
        .withVariable("id", "")
        .build()
    )

    form
  }

}
