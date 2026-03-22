package com.anjunar.technologyspeaks.followers

import com.anjunar.technologyspeaks.core.User
import com.anjunar.technologyspeaks.rest.types.Data
import com.anjunar.technologyspeaks.security.IdentityHolder
import jakarta.annotation.security.RolesAllowed
import jakarta.json.bind.annotation.JsonbProperty
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.{GetMapping, PathVariable, PutMapping, RequestBody, RestController}
import org.springframework.web.server.ResponseStatusException

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._

@RestController
class UserGroupsController(private val identityHolder: IdentityHolder) {

  @GetMapping(value = Array("/core/users/user/{id}/groups"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @Transactional(readOnly = true)
  def list(@PathVariable("id") follower: User): java.util.List[Data[Group]] = {
    val relationShip = RelationShip.query("follower" -> follower, "user" -> identityHolder.user)
    if (relationShip == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not following this user")
    }

    relationShip.groups.asScala
      .map(group => new Data(group, Group.schema()))
      .toList
      .asJava
  }

  @PutMapping(value = Array("/core/users/user/{id}/groups"), consumes = Array("application/json"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @Transactional
  def update(@PathVariable("id") follower: User, @RequestBody request: UserGroupsController.GroupAssignmentRequest): java.util.List[Data[Group]] = {
    val relationShip = RelationShip.query("follower" -> follower, "user" -> identityHolder.user)
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
      .map(group => new Data(group, Group.schema()))
      .toList
      .asJava
  }

}

object UserGroupsController {

  class GroupAssignmentRequest(
    @JsonbProperty @BeanProperty val groupIds: java.util.List[String] = new java.util.ArrayList[String]()
  ) {
    def this() = this(new java.util.ArrayList[String]())
  }

}
