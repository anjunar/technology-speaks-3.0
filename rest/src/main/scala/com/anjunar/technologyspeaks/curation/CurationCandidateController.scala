package com.anjunar.technologyspeaks.curation

import com.anjunar.technologyspeaks.core.SchemaHateoas
import com.anjunar.technologyspeaks.rest.EntityManagerProvider
import com.anjunar.technologyspeaks.rest.types.Data
import com.anjunar.technologyspeaks.security.{IdentityHolder, LinkBuilder}
import com.anjunar.technologyspeaks.shared.editor.Node
import com.anjunar.technologyspeaks.timeline.Post
import jakarta.annotation.security.RolesAllowed
import jakarta.persistence.EntityManager
import org.springframework.web.bind.annotation.{PathVariable, PostMapping, RequestBody, RestController}

import scala.jdk.CollectionConverters.*

@RestController
class CurationCandidateController(val identityHolder: IdentityHolder, val entityManager: EntityManager) extends EntityManagerProvider {

  @PostMapping(value = Array("/timeline/posts/post/{id}/curation-candidates"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def createFromPost(@PathVariable("id") post: Post): Data[CurationCandidate] = {
    existingCandidateFor(post)
      .map(candidateData)
      .getOrElse {
        val normalizedText = extractText(post.editor)
        val candidate = new CurationCandidate
        candidate.author = identityHolder.user
        candidate.source = new SourceRef("post", post.id.toString, Option(post.modified).map(_.toString).orNull)
        candidate.resonanceType = ResonanceType.Impuls
        candidate.status = CandidateStatus.Eingang
        candidate.title = Option(normalizedText).map(_.trim).filter(_.nonEmpty).map(_.take(72)).orNull
        candidate.excerpt = Option(normalizedText).map(_.trim).filter(_.nonEmpty).map(_.take(260)).getOrElse("")
        candidate.normalizedText = Option(normalizedText).map(_.trim).getOrElse("")
        candidate.priority = 50
        candidate.persist()
        candidateData(candidate)
      }
  }

  @PostMapping(value = Array("/curation/candidates/{id}/classify"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def classify(@PathVariable("id") entity: CurationCandidate, @RequestBody request: ClassifyCandidateRequest): Data[CurationCandidate] = {
    val managed = CurationCandidate.find(entity.id)
    if request != null && request.resonanceType != null && request.resonanceType.nonEmpty then
      managed.resonanceType = ResonanceType.valueOf(request.resonanceType)
    managed.status = CandidateStatus.InPruefung
    candidateData(managed)
  }

  @PostMapping(value = Array("/curation/candidates/{id}/assign-target"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def assignTarget(@PathVariable("id") entity: CurationCandidate, @RequestBody request: AssignTargetRequest): Data[CurationCandidate] = {
    val managed = CurationCandidate.find(entity.id)
    if request != null && request.documentId != null && request.documentId.nonEmpty then
      managed.target = new DocumentTarget(request.documentId, request.sectionId)
      managed.status = CandidateStatus.Zugeordnet
    candidateData(managed)
  }

  private def candidateData(candidate: CurationCandidate): Data[CurationCandidate] = {
    candidate.decisions = CurationDecision.queryAll("candidateId" -> candidate.id.toString).asScala
      .sortBy(decision => Option(decision.created))(using Ordering.Option(using Ordering[java.time.LocalDateTime]).reverse)
      .toList
      .asJava
    candidate.addLinks(
      LinkBuilder.create[CurationCandidateController](_.classify(candidate, null))
        .withRel("classify")
        .build(),
      LinkBuilder.create[CurationCandidateController](_.assignTarget(candidate, null))
        .withRel("assign-target")
        .build()
    )
    new Data(candidate, SchemaHateoas.enhance(candidate, CurationCandidate.schema))
  }

  private def existingCandidateFor(post: Post): Option[CurationCandidate] =
    CurationCandidate.findAll().asScala.find { candidate =>
      candidate.author == identityHolder.user &&
      candidate.source != null &&
      candidate.source.sourceType == "post" &&
      candidate.source.sourceId == post.id.toString &&
      candidate.source.sourceVersion == Option(post.modified).map(_.toString).orNull
    }

  private def extractText(node: Node): String =
    if node == null then ""
    else {
      val current = Option(node.text).getOrElse("")
      val nested = Option(node.content).map(_.asScala.map(extractText).mkString(" ")).getOrElse("")
      s"$current $nested".trim.replaceAll("\\s+", " ")
    }
}
