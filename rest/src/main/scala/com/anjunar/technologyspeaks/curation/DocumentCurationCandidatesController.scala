package com.anjunar.technologyspeaks.curation

import com.anjunar.technologyspeaks.core.SchemaHateoas
import com.anjunar.technologyspeaks.documents.Document
import com.anjunar.technologyspeaks.documents.DocumentController
import com.anjunar.technologyspeaks.rest.EntityManagerProvider
import com.anjunar.technologyspeaks.rest.types.{Data, Table}
import com.anjunar.technologyspeaks.security.LinkBuilder
import com.anjunar.technologyspeaks.timeline.{Post, PostController}
import jakarta.annotation.security.RolesAllowed
import jakarta.json.bind.annotation.JsonbSubtype
import jakarta.persistence.EntityManager
import org.springframework.web.bind.annotation.{GetMapping, PathVariable, RestController}

import java.util.UUID
import scala.jdk.CollectionConverters.*

@RestController
class DocumentCurationCandidatesController(val entityManager: EntityManager) extends EntityManagerProvider {

  @GetMapping(value = Array("/document/documents/document/{id}/curation-candidates"), produces = Array("application/json"))
  @RolesAllowed(Array("Anonymous", "Guest", "User", "Administrator"))
  def list(@PathVariable("id") document: Document): Table[DocumentCurationCandidatesController.CandidateRow] = {
    val rows = CurationCandidate.findAll().asScala
      .filter(candidate => candidate.target != null && candidate.target.documentId == document.id.toString)
      .sortBy(entity => Option(entity.created))(using Ordering.Option(using Ordering[java.time.LocalDateTime]).reverse)
      .map { entity =>
        entity.decisions = CurationDecision.queryAll("candidateId" -> entity.id.toString).asScala
          .sortBy(decision => Option(decision.created))(using Ordering.Option(using Ordering[java.time.LocalDateTime]).reverse)
          .toList
          .asJava
        entity.addLinks(
          LinkBuilder.create[CurationCandidateController](_.classify(entity, null))
            .withRel("classify")
            .build(),
          LinkBuilder.create[CurationCandidateController](_.assignTarget(entity, null))
            .withRel("assign-target")
            .build()
        )
        appendSourceLink(entity)
        new DocumentCurationCandidatesController.CandidateRow(entity)
      }
      .toList
      .asJava

    new Table(rows, rows.size.toLong, SchemaHateoas.enhance(new CurationCandidate, CurationCandidate.schema))
  }

  private def appendSourceLink(candidate: CurationCandidate): Unit =
    Option(candidate.source).foreach { source =>
      Option(parseUuid(source.sourceId)).foreach { id =>
        Option(source.sourceType).map(_.trim.toLowerCase) match {
          case Some("post") =>
            val post = Post.find(id)
            if (post != null) {
              candidate.addLinks(
                LinkBuilder.create[PostController](_.read(post))
                  .withRel("source")
                  .build()
              )
            }
          case Some("document") =>
            val originalDocument = Document.find(id)
            if (originalDocument != null) {
              candidate.addLinks(
                LinkBuilder.create[DocumentController](_.read(originalDocument))
                  .withRel("source")
                  .build()
              )
            }
          case _ =>
            ()
        }
      }
    }

  private def parseUuid(value: String): UUID =
    try UUID.fromString(value)
    catch {
      case _: IllegalArgumentException => null
    }
}

object DocumentCurationCandidatesController {

  @JsonbSubtype(alias = "Data", `type` = classOf[Data[?]])
  class CandidateRow(data: CurationCandidate) extends Data[CurationCandidate](data, SchemaHateoas.enhance(data, CurationCandidate.schema))
}
