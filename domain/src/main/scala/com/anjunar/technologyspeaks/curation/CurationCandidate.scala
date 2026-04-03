package com.anjunar.technologyspeaks.curation

import com.anjunar.json.mapper.provider.{EntityProvider, OwnerProvider}
import com.anjunar.json.mapper.schema.SchemaProvider
import com.anjunar.technologyspeaks.SpringContext
import com.anjunar.technologyspeaks.core.{AbstractEntity, AbstractEntitySchema, OwnerRule, User}
import com.anjunar.technologyspeaks.hibernate.{EntityContext, RepositoryContext}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.*

import scala.annotation.meta.field

@Entity
@Table(name = "Curation#Candidate")
class CurationCandidate extends AbstractEntity with EntityContext[CurationCandidate] with OwnerProvider {

  @Embedded
  @JsonbProperty
  var source: SourceRef = null

  @ManyToOne
  @JsonbProperty
  var author: User = null

  @JsonbProperty
  @Enumerated(EnumType.STRING)
  var resonanceType: ResonanceType = ResonanceType.Impuls

  @JsonbProperty
  @Enumerated(EnumType.STRING)
  var status: CandidateStatus = CandidateStatus.Eingang

  @(JsonbProperty @field)
  var title: String = null

  @(JsonbProperty @field)
  @Column(nullable = false, length = 4000)
  var excerpt: String = ""

  @(JsonbProperty @field)
  @Column(nullable = false, length = 16000)
  var normalizedText: String = ""

  @(JsonbProperty @field)
  @Column(length = 4000)
  var rationale: String = null

  @Embedded
  @JsonbProperty
  var target: DocumentTarget = null

  @(JsonbProperty @field)
  var priority: Int = 0

  @Transient
  @JsonbProperty
  var decisions: java.util.List[CurationDecision] = new java.util.ArrayList[CurationDecision]()

  override def owner(): EntityProvider = author
}

object CurationCandidate extends RepositoryContext[CurationCandidate] with SchemaProvider[CurationCandidate.Schema] {

  class Schema extends AbstractEntitySchema[CurationCandidate](SpringContext.entityManager()) {
    @JsonbProperty val source = property(_.source, classOf[OwnerRule[CurationCandidate]])
    @JsonbProperty val author = property(_.author, classOf[OwnerRule[CurationCandidate]])
    @JsonbProperty val resonanceType = property(_.resonanceType, classOf[OwnerRule[CurationCandidate]])
    @JsonbProperty val status = property(_.status, classOf[OwnerRule[CurationCandidate]])
    @JsonbProperty val title = property(_.title, classOf[OwnerRule[CurationCandidate]])
    @JsonbProperty val excerpt = property(_.excerpt, classOf[OwnerRule[CurationCandidate]])
    @JsonbProperty val normalizedText = property(_.normalizedText, classOf[OwnerRule[CurationCandidate]])
    @JsonbProperty val rationale = property(_.rationale, classOf[OwnerRule[CurationCandidate]])
    @JsonbProperty val target = property(_.target, classOf[OwnerRule[CurationCandidate]])
    @JsonbProperty val priority = property(_.priority, classOf[OwnerRule[CurationCandidate]])
    @JsonbProperty val decisions = property(_.decisions, classOf[OwnerRule[CurationCandidate]])
  }

}
