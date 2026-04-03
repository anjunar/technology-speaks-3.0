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
class CurationClustersController(val identityHolder: IdentityHolder, val entityManager: EntityManager) extends EntityManagerProvider {

  @GetMapping(value = Array("/curation/clusters"), produces = Array("application/json"))
  @RolesAllowed(Array("Anonymous", "Guest", "User", "Administrator"))
  def list(search: ClusterSearch): Table[CurationClustersController.ClusterRow] = {
    val filtered = CurationCluster.findAll().asScala
      .sortBy(entity => Option(entity.created))(using Ordering.Option(using Ordering[java.time.LocalDateTime]).reverse)
      .filter(cluster => matchesStatus(cluster, search.status))
      .filter(cluster => matchesQuery(cluster, search.query))
      .toList

    val rows = filtered
      .slice(search.index, search.index + search.limit)
      .map { entity =>
        refreshMetrics(entity)
        entity.addLinks(
          LinkBuilder.create[CurationClusterController](_.addCandidate(entity, null))
            .withRel("add-candidate")
            .build(),
          LinkBuilder.create[CurationClusterController](_.writeSummary(entity, null))
            .withRel("write-summary")
            .build(),
          LinkBuilder.create[CurationClusterController](_.accept(entity, null))
            .withRel("accept")
            .build(),
          LinkBuilder.create[CurationClusterController](_.defer(entity, null))
            .withRel("defer")
            .build(),
          LinkBuilder.create[CurationClusterController](_.reject(entity, null))
            .withRel("reject")
            .build()
        )
        new CurationClustersController.ClusterRow(entity)
      }
      .toList
      .asJava

    new Table(rows, filtered.size.toLong, SchemaHateoas.enhance(new CurationCluster, CurationCluster.schema))
  }

  private def matchesStatus(cluster: CurationCluster, value: String): Boolean =
    value == null || value.isBlank || cluster.status.toString == value

  private def matchesQuery(cluster: CurationCluster, value: String): Boolean =
    value == null ||
      value.isBlank ||
      Option(cluster.title).exists(_.toLowerCase.contains(value.trim.toLowerCase)) ||
      Option(cluster.summary).exists(_.toLowerCase.contains(value.trim.toLowerCase))

  private def refreshMetrics(cluster: CurationCluster): Unit = {
    val candidates = cluster.candidateIds.asScala.flatMap(id => Option(CurationCandidate.find(id)))
    cluster.contradictionCount = candidates.count(_.resonanceType == ResonanceType.Einwand)
    cluster.questionCount = candidates.count(_.resonanceType == ResonanceType.Frage)
  }
}

object CurationClustersController {

  @JsonbSubtype(alias = "Data", `type` = classOf[Data[?]])
  class ClusterRow(data: CurationCluster) extends Data[CurationCluster](data, SchemaHateoas.enhance(data, CurationCluster.schema))
}
