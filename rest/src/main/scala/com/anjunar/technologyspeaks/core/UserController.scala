package com.anjunar.technologyspeaks.core

import com.anjunar.technologyspeaks.followers.UserGroupsController.GroupAssignmentRequest
import com.anjunar.technologyspeaks.followers.{FollowerController, RelationShip, UserGroupsController}
import com.anjunar.technologyspeaks.rest.EntityGraph
import com.anjunar.technologyspeaks.rest.types.Data
import com.anjunar.technologyspeaks.security.{IdentityHolder, LinkBuilder}
import jakarta.annotation.security.RolesAllowed
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.{DeleteMapping, GetMapping, PathVariable, PutMapping, RequestBody, RestController}

@RestController
class UserController(val identityHolder: IdentityHolder, val userService: UserService) {

  @GetMapping(value = Array("/core/users/user/{id}"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @EntityGraph("User.full")
  def read(@PathVariable("id") user: User): Data[User] = {
    val isOwnProfile = identityHolder.user != null && identityHolder.user.id == user.id
    val form = new Data(user, SchemaHateoas.enhance(user, User.schema))


    user.addLinks(
      LinkBuilder.create[UserController](_.delete(new User("")))
        .build()
    )

    if (isOwnProfile) {
      user.addLinks(
        LinkBuilder.create[UserController](_.update(new User("")))
          .build()
      )
    }

    if (!isOwnProfile) {
      val relationShip =
        if (identityHolder.user == null || user == null) null
        else RelationShip.query("follower" -> user, "user" -> identityHolder.user)

      user.addLinks(
        if (relationShip == null) {
          LinkBuilder.create[FollowerController](_.follow(user))
            .withRel("follow")
            .build()
        } else {
          LinkBuilder.create[FollowerController](_.unfollow(user))
            .withRel("unfollow")
            .build()
        }
      )

      if (relationShip != null) {
        user.addLinks(
          LinkBuilder.create[UserGroupsController](_.list(user))
            .withRel("groups")
            .build(),
          LinkBuilder.create[UserGroupsController](_.update(user, null))
            .withRel("updateGroups")
            .build()
        )
      }
    }

    form
  }

  @PutMapping(value = Array("/core/users/user"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @EntityGraph("User.full")
  def update(@RequestBody user: User): Data[User] = {
    val managedUser = user.merge()
    val form = new Data(managedUser, SchemaHateoas.enhance(managedUser, User.schema))

    managedUser.addLinks(
      LinkBuilder.create[UserController](_.update(new User("")))
        .build(),
      LinkBuilder.create[ManagedPropertyController](_.read(new ManagedProperty("")))
        .build()
    )

    form
  }

  @DeleteMapping(value = Array("/core/users/user"), consumes = Array("application/json"))
  @RolesAllowed(Array("Administrator"))
  @EntityGraph("User.full")
  def delete(@RequestBody user: User): ResponseEntity[Unit] = {

    userService.deleteUser(user)
    ResponseEntity.ok().build()

  }


}
