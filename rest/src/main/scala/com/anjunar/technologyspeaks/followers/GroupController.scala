package com.anjunar.technologyspeaks.followers

import com.anjunar.technologyspeaks.rest.types.Data
import com.anjunar.technologyspeaks.security.{IdentityHolder, LinkBuilder}
import jakarta.annotation.security.RolesAllowed
import org.springframework.http.{HttpStatus, ResponseEntity}
import org.springframework.web.bind.annotation.{DeleteMapping, GetMapping, PathVariable, PostMapping, PutMapping, RequestBody, RestController}
import org.springframework.web.server.ResponseStatusException

@RestController
class GroupController(private val identityHolder: IdentityHolder) {

  @GetMapping(value = Array("/followers/groups/groups/{id}"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def read(@PathVariable("id") entity: Group): Data[Group] = {
    if (entity.user.id != identityHolder.user.id) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
    }

    entity.addLinks(
      LinkBuilder.create[GroupController](_.update(null)).build(),
      LinkBuilder.create[GroupController](_.delete(null)).build()
    )

    new Data(entity, Group.schema)
  }

  @PostMapping(value = Array("/followers/groups/groups"), consumes = Array("application/json"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def save(@RequestBody entity: Group): Data[Group] = {
    entity.user = identityHolder.user
    entity.persist()

    entity.addLinks(
      LinkBuilder.create[GroupController](_.update(null)).build(),
      LinkBuilder.create[GroupController](_.delete(null)).build()
    )

    new Data(entity, Group.schema)
  }

  @PutMapping(value = Array("/followers/groups/groups"), consumes = Array("application/json"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def update(@RequestBody entity: Group): Data[Group] = {
    val managed = Group.find(entity.id)
    if (managed == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found")
    }
    if (managed.user.id != identityHolder.user.id) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
    }

    managed.name = entity.name

    managed.addLinks(
      LinkBuilder.create[GroupController](_.update(null)).build(),
      LinkBuilder.create[GroupController](_.delete(null)).build()
    )

    new Data(managed, Group.schema)
  }

  @DeleteMapping(value = Array("/followers/groups/groups"), consumes = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def delete(@RequestBody entity: Group): ResponseEntity[Void] = {
    val managed = Group.find(entity.id)
    if (managed == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found")
    }
    if (managed.user.id != identityHolder.user.id) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
    }

    managed.remove()
    ResponseEntity.ok().build()
  }

}
