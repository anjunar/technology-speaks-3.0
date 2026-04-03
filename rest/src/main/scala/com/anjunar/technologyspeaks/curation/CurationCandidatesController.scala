package com.anjunar.technologyspeaks.curation

import com.anjunar.technologyspeaks.core.SchemaHateoas
import com.anjunar.technologyspeaks.rest.EntityManagerProvider
import com.anjunar.technologyspeaks.rest.types.{Data, Table}
import com.anjunar.technologyspeaks.security.{IdentityHolder, LinkBuilder}
import jakarta.annotation.security.RolesAllowed
import jakarta.json.bind.annotation.JsonbSubtype
import jakarta.persistence.EntityManager
import org.springframework.web.bind.annotation.{GetMapping, RestController}

import scala.jdk.CollectionConverters.*

@RestController
class CurationCandidatesController(val identityHolder: IdentityHolder, val entityManager: EntityManager) extends EntityManagerProvider {

  @GetMapping(value = Array("/curation/candidates"), produces = Array("application/json"))
  @RolesAllowed(Array("Anonymous", "Guest", "User", "Administrator"))
  def list(search: CandidateSearch): Table[CurationCandidatesController.CandidateRow] = {
    val filtered = CurationCandidate.findAll().asScala
      .sortBy(entity => Option(entity.created))(using Ordering.Option(using Ordering[java.time.LocalDateTime]).reverse)
      .filter(candidate => matchesStatus(candidate, search.status))
      .filter(candidate => matchesResonanceType(candidate, search.resonanceType))
      .filter(candidate => matchesQuery(candidate, search.query))
      .toList

    val rows = filtered
      .slice(search.index, search.index + search.limit)
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
        new CurationCandidatesController.CandidateRow(entity)
      }
      .toList
      .asJava

    new Table(rows, filtered.size.toLong, SchemaHateoas.enhance(new CurationCandidate, CurationCandidate.schema))
  }

  private def matchesStatus(candidate: CurationCandidate, value: String): Boolean =
    value == null || value.isBlank || candidate.status.toString == value

  private def matchesResonanceType(candidate: CurationCandidate, value: String): Boolean =
    value == null || value.isBlank || candidate.resonanceType.toString == value

  private def matchesQuery(candidate: CurationCandidate, value: String): Boolean =
    value == null ||
      value.isBlank ||
      Option(candidate.title).exists(_.toLowerCase.contains(value.trim.toLowerCase)) ||
      Option(candidate.excerpt).exists(_.toLowerCase.contains(value.trim.toLowerCase))
}

object CurationCandidatesController {

  @JsonbSubtype(alias = "Data", `type` = classOf[Data[?]])
  class CandidateRow(data: CurationCandidate) extends Data[CurationCandidate](data, SchemaHateoas.enhance(data, CurationCandidate.schema))
}
