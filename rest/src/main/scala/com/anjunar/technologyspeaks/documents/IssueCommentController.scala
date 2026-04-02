package com.anjunar.technologyspeaks.documents

import com.anjunar.technologyspeaks.core.SchemaHateoas
import com.anjunar.technologyspeaks.rest.EntityManagerProvider
import com.anjunar.technologyspeaks.rest.types.Data
import com.anjunar.technologyspeaks.security.IdentityHolder
import com.anjunar.technologyspeaks.shared.commentable.FirstComment
import jakarta.annotation.security.RolesAllowed
import jakarta.persistence.EntityManager
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.{DeleteMapping, PathVariable, PostMapping, PutMapping, RequestBody, RestController}

import scala.jdk.CollectionConverters.*

@RestController
class IssueCommentController(val identityHolder: IdentityHolder, val entityManager : EntityManager) extends EntityManagerProvider {

  @PostMapping(value = Array("/document/documents/document/issues/issue/{id}/comment"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def save(@PathVariable("id") post: Issue, @RequestBody body: FirstComment): Data[FirstComment] = {
    body.user = identityHolder.user
    for (comment <- body.comments.asScala if isUserEmpty(comment)) {
      comment.user = identityHolder.user
    }
    body.persist()

    post.comments.add(body)

    new Data(body, SchemaHateoas.enhance(body, FirstComment.schema))
  }

  @PutMapping(value = Array("/document/documents/document/issues/issue/{id}/comment"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def update(@PathVariable("id") post: Issue, @RequestBody body: FirstComment): Data[FirstComment] = {
    for (comment <- body.comments.asScala if isUserEmpty(comment)) {
      comment.user = identityHolder.user
    }

    val managed = body.merge()
    post.comments.add(managed)

    new Data(managed, SchemaHateoas.enhance(managed, FirstComment.schema))
  }

  @DeleteMapping(value = Array("/document/documents/document/issues/issue/{id}/comment"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def delete(@PathVariable("id") post: Issue, @RequestBody body: FirstComment): ResponseEntity[Void] = {
    post.comments.remove(body)

    body.remove()

    ResponseEntity.ok().build()
  }

  private def isUserEmpty(comment: com.anjunar.technologyspeaks.shared.commentable.SecondComment): Boolean = {
    try {
      comment.user == null
    } catch {
      case _: Exception => true
    }
  }

}
