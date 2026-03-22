package com.anjunar.technologyspeaks.followers

import com.anjunar.technologyspeaks.core.User
import com.anjunar.technologyspeaks.security.IdentityHolder
import jakarta.annotation.security.RolesAllowed
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.{PathVariable, PostMapping, RestController}

@RestController
class FollowerController(val identityHolder: IdentityHolder) {

  @PostMapping(Array("/core/users/user/{id}/follow"))
  @RolesAllowed(Array("User", "Administrator"))
  def follow(@PathVariable("id") entity: User): ResponseEntity[Void] = {
    val relationShip = new RelationShip
    relationShip.follower = entity
    relationShip.user = identityHolder.user
    relationShip.persist()
    ResponseEntity.ok().build()
  }

  @PostMapping(Array("/core/users/user/{id}/unfollow"))
  @RolesAllowed(Array("User", "Administrator"))
  def unfollow(@PathVariable("id") entity: User): ResponseEntity[Void] = {
    val result = RelationShip.query("follower" -> entity, "user" -> identityHolder.user)
    if (result != null) {
      result.remove()
    }
    ResponseEntity.ok().build()
  }

}
