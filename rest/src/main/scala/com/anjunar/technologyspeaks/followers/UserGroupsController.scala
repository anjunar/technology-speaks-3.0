package com.anjunar.technologyspeaks.followers

import com.anjunar.technologyspeaks.core.SchemaHateoas
import com.anjunar.technologyspeaks.core.User
import com.anjunar.technologyspeaks.rest.EntityManagerProvider
import com.anjunar.technologyspeaks.rest.types.Data
import com.anjunar.technologyspeaks.security.IdentityHolder
import jakarta.annotation.security.RolesAllowed
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.EntityManager
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.{GetMapping, PathVariable, PutMapping, RequestBody, RestController}
import org.springframework.web.server.ResponseStatusException

import java.util.UUID
import scala.jdk.CollectionConverters.*

@RestController
class UserGroupsController(private val identityHolder: IdentityHolder, val entityManager : EntityManager) extends EntityManagerProvider {

  @GetMapping(value = Array("/core/users/user/{id}/groups"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @Transactional(readOnly = true)
  def list(@PathVariable("id") follower: User): java.util.List[Data[Group]] = {
    val relationShip = findRelationShip(follower)
    if (relationShip == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not following this user")
    }

    relationShip.groups.asScala
      .map(group => new Data(group, SchemaHateoas.enhance(group, Group.schema)))
      .toList
      .asJava
  }

  @PutMapping(value = Array("/core/users/user/{id}/groups"), consumes = Array("application/json"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @Transactional
  def update(@PathVariable("id") follower: User, @RequestBody request: UserGroupsController.GroupAssignmentRequest): java.util.List[Data[Group]] = {
    val relationShip = findRelationShip(follower)
    if (relationShip == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not following this user")
    }

    val resolved = request.groupIds.asScala.distinct.map { groupId =>
      val group = Group.find(groupId)
      if (group == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found")
      }
      if (group.user.id != identityHolder.user.id) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
      }
      group
    }

    relationShip.groups.clear()
    relationShip.groups.addAll(resolved.asJava)

    relationShip.groups.asScala
      .map(group => new Data(group, SchemaHateoas.enhance(group, Group.schema)))
      .toList
      .asJava
  }

  private def findRelationShip(follower: User): RelationShip =
    if (follower == null || identityHolder.user == null) null
    else RelationShip.query("follower" -> follower, "user" -> identityHolder.user)

}

object UserGroupsController {

  class GroupAssignmentRequest(
    @JsonbProperty val groupIds: java.util.List[UUID] = new java.util.ArrayList[UUID]()
  ) {
    def this() = this(new java.util.ArrayList[UUID]())
  }

}
