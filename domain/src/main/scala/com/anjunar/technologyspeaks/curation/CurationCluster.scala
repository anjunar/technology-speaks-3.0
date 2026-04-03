package com.anjunar.technologyspeaks.curation

import com.anjunar.json.mapper.schema.SchemaProvider
import com.anjunar.technologyspeaks.SpringContext
import com.anjunar.technologyspeaks.core.{AbstractEntity, AbstractEntitySchema}
import com.anjunar.technologyspeaks.hibernate.{EntityContext, RepositoryContext}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.*

import scala.annotation.meta.field
import scala.jdk.CollectionConverters.*

@Entity
@Table(name = "Curation#Cluster")
class CurationCluster extends AbstractEntity with EntityContext[CurationCluster] {

  @(JsonbProperty @field)
  @Column(nullable = false)
  var title: String = ""

  @(JsonbProperty @field)
  @Column(length = 4000)
  var summary: String = null

  @JsonbProperty
  @Enumerated(EnumType.STRING)
  var status: ClusterStatus = ClusterStatus.Offen

  @Embedded
  @JsonbProperty
  var target: DocumentTarget = null

  @ElementCollection
  @CollectionTable(name = "Curation#Cluster#CandidateIds", joinColumns = Array(new JoinColumn(name = "cluster_id")))
  @Column(name = "candidate_id")
  @JsonbProperty
  val candidateIds: java.util.List[String] = new java.util.ArrayList[String]()

  @(JsonbProperty @field)
  var contradictionCount: Int = 0

  @(JsonbProperty @field)
  var questionCount: Int = 0

  @(JsonbProperty @field)
  var acceptedCount: Int = 0

  @(JsonbProperty @field)
  var rejectedCount: Int = 0
}

object CurationCluster extends RepositoryContext[CurationCluster] with SchemaProvider[CurationCluster.Schema] {

  class Schema extends AbstractEntitySchema[CurationCluster](SpringContext.entityManager()) {
    @JsonbProperty val title = property(_.title)
    @JsonbProperty val summary = property(_.summary)
    @JsonbProperty val status = property(_.status)
    @JsonbProperty val target = property(_.target)
    @JsonbProperty val candidateIds = property(_.candidateIds)
    @JsonbProperty val contradictionCount = property(_.contradictionCount)
    @JsonbProperty val questionCount = property(_.questionCount)
    @JsonbProperty val acceptedCount = property(_.acceptedCount)
    @JsonbProperty val rejectedCount = property(_.rejectedCount)
  }

}
