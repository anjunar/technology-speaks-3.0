package com.anjunar.technologyspeaks.documents

import com.anjunar.technologyspeaks.core.SchemaHateoas
import com.anjunar.technologyspeaks.curation.DocumentCurationCandidatesController
import com.anjunar.technologyspeaks.rest.{EntityGraph, EntityManagerProvider}
import com.anjunar.technologyspeaks.rest.types.Data
import com.anjunar.technologyspeaks.security.{IdentityHolder, LinkBuilder}
import com.anjunar.technologyspeaks.shared.editor.Node
import jakarta.annotation.security.RolesAllowed
import jakarta.persistence.EntityManager
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.bind.annotation.{GetMapping, PathVariable, PostMapping, PutMapping, RequestBody, RestController}

@RestController
class DocumentController(val identityHolder: IdentityHolder, val documentImportService: DocumentImportService, val entityManager : EntityManager)
  extends EntityManagerProvider {

  @GetMapping(value = Array("/document/documents/document"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @EntityGraph("Document.full")
  def create(): Data[Document] = {
    val entity = new Document("Arbeitsblatt")
    entity.user = identityHolder.user
    val node = new Node
    node.nodeType = "doc"
    entity.editor = node

    val form = new Data(entity, SchemaHateoas.enhance(entity, Document.schema))

    entity.addLinks(
      LinkBuilder.create[DocumentController](_.save(null))
        .build()
    )

    form
  }

  @PostMapping(value = Array("/document/documents/document/root"), produces = Array("application/json"))
  @RolesAllowed(Array("Anonymous", "Guest", "User", "Administrator"))
  @EntityGraph("Document.full")
  def root(): Data[Document] = {
    var entity = Document.query("title" -> "Technology Speaks")

    if (entity == null) {
      entity = new Document("Technology Speaks")
      entity.user = identityHolder.user
      val node = new Node
      node.nodeType = "doc"
      entity.editor = node
      entity.persist()
    }

    val form = new Data(entity, SchemaHateoas.enhance(entity, Document.schema))

    entity.addLinks(
      LinkBuilder.create[DocumentController](_.create())
        .withRel("create-document")
        .build(),
      LinkBuilder.create[DocumentCurationCandidatesController](_.list(entity))
        .withRel("curation-candidates")
        .build(),
      LinkBuilder.create[IssuesController](_.list(new IssueSearch(entity)))
        .build(),
      LinkBuilder.create[IssueController](_.create(entity))
        .withRel("create-issue")
        .build()
    )

    if (identityHolder.hasRole("Administrator")) {
      entity.addLinks(
        LinkBuilder.create[DocumentController](_.importFromDirectory(null))
          .withRel("import-documents")
          .build()
      )
    }

    if (identityHolder.user == entity.user) {
      entity.addLinks(
        LinkBuilder.create[DocumentController](_.update(null))
          .build()
      )
    }

    form
  }

  @GetMapping(value = Array("/document/documents/document/{id}"), produces = Array("application/json"))
  @RolesAllowed(Array("Anonymous", "Guest", "User", "Administrator"))
  @EntityGraph("Document.full")
  def read(@PathVariable("id") entity: Document): Data[Document] = {
    val form = new Data(entity, SchemaHateoas.enhance(entity, Document.schema))

    entity.addLinks(
      LinkBuilder.create[DocumentCurationCandidatesController](_.list(entity))
        .withRel("curation-candidates")
        .build(),
      LinkBuilder.create[IssuesController](_.list(new IssueSearch(entity)))
        .build(),
      LinkBuilder.create[IssueController](_.create(entity))
        .build()
    )

    if (identityHolder.hasRole("Administrator")) {
      entity.addLinks(
        LinkBuilder.create[DocumentController](_.importFromDirectory(null))
          .withRel("import-documents")
          .build()
      )
    }

    if (identityHolder.user == entity.user) {
      entity.addLinks(
        LinkBuilder.create[DocumentController](_.update(null))
          .build()
      )
    }

    form
  }

  @PostMapping(value = Array("/document/documents/document"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @EntityGraph("Document.full")
  def save(@RequestBody entity: Document): Data[Document] = {
    entity.user = identityHolder.user
    entity.persist()

    val form = new Data(entity, SchemaHateoas.enhance(entity, Document.schema))

    entity.addLinks(
      LinkBuilder.create[DocumentController](_.update(null))
        .build(),
      LinkBuilder.create[DocumentCurationCandidatesController](_.list(entity))
        .withRel("curation-candidates")
        .build(),
      LinkBuilder.create[IssuesController](_.list(new IssueSearch(entity)))
        .build(),
      LinkBuilder.create[IssueController](_.create(entity))
        .withVariable("id", entity.id)
        .build()
    )

    form
  }

  @PutMapping(value = Array("/document/documents/document"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @EntityGraph("Document.full")
  def update(@RequestBody entity: Document): Data[Document] = {
    val managed = Document.find(entity.id)
    if (managed == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")
    }
    if (managed.user != identityHolder.user) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
    }

    managed.title = entity.title
    managed.editor = entity.editor

    val form = new Data(managed, SchemaHateoas.enhance(managed, Document.schema))

    managed.addLinks(
      LinkBuilder.create[DocumentController](_.update(null))
        .build(),
      LinkBuilder.create[DocumentCurationCandidatesController](_.list(managed))
        .withRel("curation-candidates")
        .build(),
      LinkBuilder.create[IssuesController](_.list(new IssueSearch(managed)))
        .build(),
      LinkBuilder.create[IssueController](_.create(managed))
        .build()
    )

    form
  }

  @PostMapping(value = Array("/document/documents/import"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("Administrator"))
  def importFromDirectory(@RequestBody request: DocumentImportService.ImportRequest): DocumentImportService.ImportResult =
    documentImportService.importDirectory(request)

}
