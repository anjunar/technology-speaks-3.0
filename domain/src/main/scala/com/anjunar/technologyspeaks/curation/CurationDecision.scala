package com.anjunar.technologyspeaks.curation

import com.anjunar.json.mapper.schema.SchemaProvider
import com.anjunar.technologyspeaks.SpringContext
import com.anjunar.technologyspeaks.core.{AbstractEntity, AbstractEntitySchema}
import com.anjunar.technologyspeaks.hibernate.RepositoryContext
import com.anjunar.technologyspeaks.hibernate.EntityContext
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.{Column, Entity, EnumType, Enumerated, Table}

import scala.annotation.meta.field

@Entity
@Table(name = "Curation#Decision")
class CurationDecision(
  @(JsonbProperty @field)
  @(Column @field)(nullable = false)
  var clusterId: String
) extends AbstractEntity with EntityContext[CurationDecision] {

  def this() = this(null)

  @(JsonbProperty @field)
  var candidateId: String = null

  @(JsonbProperty @field)
  @(Enumerated @field)(EnumType.STRING)
  var decisionType: DecisionType = DecisionType.Rueckfrage

  @(JsonbProperty @field)
  @Column(length = 4000)
  var note: String = null

  @(JsonbProperty @field)
  var decidedBy: String = null
}

object CurationDecision extends RepositoryContext[CurationDecision] with SchemaProvider[CurationDecision.Schema] {

  class Schema extends AbstractEntitySchema[CurationDecision](SpringContext.entityManager()) {
    @JsonbProperty val clusterId = property(_.clusterId)
    @JsonbProperty val candidateId = property(_.candidateId)
    @JsonbProperty val decisionType = property(_.decisionType)
    @JsonbProperty val note = property(_.note)
    @JsonbProperty val decidedBy = property(_.decidedBy)
  }

}
