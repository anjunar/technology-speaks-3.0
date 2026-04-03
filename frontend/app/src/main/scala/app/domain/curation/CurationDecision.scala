package app.domain.curation

import app.domain.core.AbstractEntity
import jfx.core.state.Property

class CurationDecision extends AbstractEntity {
  val clusterId: Property[String] = Property("")
  val candidateId: Property[String | Null] = Property(null)
  val decisionType: Property[String] = Property(DecisionType.Rueckfrage)
  val note: Property[String | Null] = Property(null)
  val decidedBy: Property[String] = Property("")
  val decidedAt: Property[String | Null] = Property(null)
}
