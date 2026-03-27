package com.anjunar.technologyspeaks.documents

import com.anjunar.technologyspeaks.core.SchemaHateoas
import com.anjunar.technologyspeaks.rest.EntityGraph
import com.anjunar.technologyspeaks.rest.types.Data
import com.anjunar.technologyspeaks.security.{IdentityHolder, LinkBuilder}
import jakarta.annotation.security.RolesAllowed
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation._

@RestController
class IssueController(val identityHolder: IdentityHolder) {

  @GetMapping(value = Array("/document/documents/document/{id}/issues/issue"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @EntityGraph("Issue.full")
  def create(@PathVariable("id") document: Document): Data[Issue] = {
    val entity = new Issue("Neue Aufgabe")

    entity.addLinks(
      LinkBuilder.create[IssueController](_.save(document, null))
        .build()
    )

    new Data(entity, SchemaHateoas.enhance(entity, Issue.schema))
  }

  @GetMapping(value = Array("/document/documents/document/{document}/issues/issue/{id}"), produces = Array("application/json"))
  @RolesAllowed(Array("Anonymous", "Guest", "User", "Administrator"))
  @EntityGraph("Issue.full")
  def read(@PathVariable("document") document: Document, @PathVariable("id") entity: Issue): Data[Issue] = {
    if (identityHolder.user == entity.user) {
      entity.addLinks(
        LinkBuilder.create[IssueController](_.update(entity.document, null))
          .build(),
        LinkBuilder.create[IssueController](_.delete(null))
          .build()
      )
    }

    entity.addLinks(
      LinkBuilder.create[IssueCommentsController](_.comments(new IssueCommentSearch(entity)))
        .build(),
      LinkBuilder.create[IssueCommentController](_.save(entity, null))
        .build()
    )

    new Data(entity, SchemaHateoas.enhance(entity, Issue.schema))
  }

  @PostMapping(value = Array("/document/documents/document/{id}/issues/issue"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @EntityGraph("Issue.full")
  def save(@PathVariable("id") document: Document, @RequestBody entity: Issue): Data[Issue] = {
    entity.document = document
    entity.user = identityHolder.user
    entity.persist()

    entity.addLinks(
      LinkBuilder.create[IssueController](_.update(entity.document, null))
        .build(),
      LinkBuilder.create[IssueController](_.delete(null))
        .build()
    )

    new Data(entity, SchemaHateoas.enhance(entity, Issue.schema))
  }

  @PutMapping(value = Array("/document/documents/document/{id}/issues/issue"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @EntityGraph("Issue.full")
  def update(@PathVariable("id") document: Document, @RequestBody entity: Issue): Data[Issue] = {
    entity.document = document
    entity.user = identityHolder.user

    entity.addLinks(
      LinkBuilder.create[IssueController](_.update(entity.document, null))
        .build(),
      LinkBuilder.create[IssueController](_.delete(null))
        .build()
    )

    new Data(entity, SchemaHateoas.enhance(entity, Issue.schema))
  }

  @DeleteMapping(value = Array("/document/documents/document/issues/issue"), consumes = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @EntityGraph("Issue.full")
  def delete(@RequestBody entity: Issue): ResponseEntity[Void] = {
    entity.remove()
    ResponseEntity.ok().build()
  }

}
