package com.anjunar.technologyspeaks.curation

import com.anjunar.technologyspeaks.core.{SchemaHateoas, User}
import com.anjunar.technologyspeaks.rest.EntityManagerProvider
import com.anjunar.technologyspeaks.rest.types.Data
import com.anjunar.technologyspeaks.security.IdentityHolder
import com.anjunar.technologyspeaks.security.LinkBuilder
import jakarta.persistence.EntityManager
import jakarta.annotation.security.RolesAllowed
import org.springframework.web.bind.annotation.{PathVariable, PostMapping, RequestBody, RestController}

import scala.jdk.CollectionConverters.*

@RestController
class CurationClusterController(val entityManager: EntityManager, val identityHolder: IdentityHolder) extends EntityManagerProvider {

  @PostMapping(value = Array("/curation/clusters"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def create(@RequestBody request: CreateClusterRequest): Data[CurationCluster] = {
    val entity = new CurationCluster
    entity.title = Option(request).map(_.title).map(_.trim).filter(_.nonEmpty).getOrElse("Neuer Cluster")
    entity.status = ClusterStatus.Offen
    entity.persist()
    clusterData(entity)
  }

  @PostMapping(value = Array("/curation/clusters/{id}/add-candidate"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def addCandidate(@PathVariable("id") entity: CurationCluster, @RequestBody request: AddCandidateRequest): Data[CurationCluster] = {
    val managed = CurationCluster.find(entity.id)
    if request != null && request.candidateId != null && request.candidateId.nonEmpty && !managed.candidateIds.contains(request.candidateId) then
      managed.candidateIds.add(request.candidateId)
    refreshMetrics(managed)
    clusterData(managed)
  }

  @PostMapping(value = Array("/curation/clusters/{id}/write-summary"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def writeSummary(@PathVariable("id") entity: CurationCluster, @RequestBody request: WriteSummaryRequest): Data[CurationCluster] = {
    val managed = CurationCluster.find(entity.id)
    if request != null then
      managed.summary = request.summary
    clusterData(managed)
  }

  @PostMapping(value = Array("/curation/clusters/{id}/accept"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def accept(@PathVariable("id") entity: CurationCluster, @RequestBody request: DecisionNoteRequest): Data[CurationCluster] = {
    val managed = CurationCluster.find(entity.id)
    applyStatus(managed, CandidateStatus.Uebernommen, DecisionType.Uebernommen, request)
    managed.status = ClusterStatus.Abgeschlossen
    managed.acceptedCount = managed.acceptedCount + 1
    clusterData(managed)
  }

  @PostMapping(value = Array("/curation/clusters/{id}/defer"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def defer(@PathVariable("id") entity: CurationCluster, @RequestBody request: DecisionNoteRequest): Data[CurationCluster] = {
    val managed = CurationCluster.find(entity.id)
    applyStatus(managed, CandidateStatus.Zurueckgestellt, DecisionType.Zurueckgestellt, request)
    managed.status = ClusterStatus.InBearbeitung
    clusterData(managed)
  }

  @PostMapping(value = Array("/curation/clusters/{id}/reject"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def reject(@PathVariable("id") entity: CurationCluster, @RequestBody request: DecisionNoteRequest): Data[CurationCluster] = {
    val managed = CurationCluster.find(entity.id)
    applyStatus(managed, CandidateStatus.Verworfen, DecisionType.Verworfen, request)
    managed.status = ClusterStatus.Abgeschlossen
    managed.rejectedCount = managed.rejectedCount + 1
    clusterData(managed)
  }

  private def clusterData(cluster: CurationCluster): Data[CurationCluster] = {
    refreshMetrics(cluster)
    cluster.addLinks(
      LinkBuilder.create[CurationClusterController](_.addCandidate(cluster, null))
        .withRel("add-candidate")
        .build(),
      LinkBuilder.create[CurationClusterController](_.writeSummary(cluster, null))
        .withRel("write-summary")
        .build(),
      LinkBuilder.create[CurationClusterController](_.accept(cluster, null))
        .withRel("accept")
        .build(),
      LinkBuilder.create[CurationClusterController](_.defer(cluster, null))
        .withRel("defer")
        .build(),
      LinkBuilder.create[CurationClusterController](_.reject(cluster, null))
        .withRel("reject")
        .build()
    )
    new Data(cluster, SchemaHateoas.enhance(cluster, CurationCluster.schema))
  }

  private def refreshMetrics(cluster: CurationCluster): Unit = {
    val candidates = cluster.candidateIds.asScala.flatMap(id => Option(CurationCandidate.find(id)))
    cluster.contradictionCount = candidates.count(_.resonanceType == ResonanceType.Einwand)
    cluster.questionCount = candidates.count(_.resonanceType == ResonanceType.Frage)
  }

  private def applyStatus(cluster: CurationCluster, status: CandidateStatus, decisionType: DecisionType, request: DecisionNoteRequest): Unit =
    cluster.candidateIds.asScala.foreach { id =>
      Option(CurationCandidate.find(id)).foreach { candidate =>
        candidate.status = status
        createDecision(cluster, candidate, decisionType, request)
      }
    }

  private def createDecision(cluster: CurationCluster, candidate: CurationCandidate, decisionType: DecisionType, request: DecisionNoteRequest): Unit = {
    val decision = new CurationDecision(cluster.id.toString)
    decision.candidateId = candidate.id.toString
    decision.decisionType = decisionType
    decision.note = Option(request).map(_.note).map(_.trim).filter(_.nonEmpty).orNull
    decision.decidedBy = renderDecidedBy(identityHolder.user)
    decision.persist()
  }

  private def renderDecidedBy(user: User): String =
    if user == null then ""
    else Option(user.nickName).filter(_.trim.nonEmpty).getOrElse(user.id.toString)
}
