package app.domain.curation

import app.domain.core.{AbstractEntity, Data, Table, User}
import app.domain.documents.Document
import app.support.Api
import app.support.Api.given
import app.support.Navigation
import jfx.core.state.{ListProperty, Property}

import scala.concurrent.Future
import scala.scalajs.js.URIUtils.encodeURIComponent

class CurationCandidate extends AbstractEntity {
  val source: Property[SourceRef | Null] = Property(null)
  val author: Property[User | Null] = Property(null)
  val resonanceType: Property[String] = Property(ResonanceType.Impuls)
  val status: Property[String] = Property(CandidateStatus.Eingang)
  val title: Property[String | Null] = Property(null)
  val excerpt: Property[String] = Property("")
  val normalizedText: Property[String] = Property("")
  val rationale: Property[String | Null] = Property(null)
  val target: Property[DocumentTarget | Null] = Property(null)
  val priority: Property[Int] = Property(0)
  val decisions: ListProperty[CurationDecision] = ListProperty()

  def classify(nextType: String): Future[Data[CurationCandidate]] =
    Api.request(s"/service/curation/candidates/${id.get}/classify")
      .post(new ClassifyCandidateRequest(nextType))
      .read[Data[CurationCandidate]]

  def assignTarget(documentId: String, sectionId: String | Null): Future[Data[CurationCandidate]] =
    Api.request(s"/service/curation/candidates/${id.get}/assign-target")
      .post(new AssignTargetRequest(documentId, sectionId))
      .read[Data[CurationCandidate]]
}

object CurationCandidate {
  def listForDocument(document: Document, index: Int, limit: Int): Future[Table[Data[CurationCandidate]]] =
    Navigation.linkByRel("curation-candidates", document.links)
      .map(link => Api.link(link).invoke.read[Table[Data[CurationCandidate]]])
      .getOrElse {
        Api.request(s"/service/document/documents/document/${document.id.get}/curation-candidates")
          .get
          .read[Table[Data[CurationCandidate]]]
      }

  def list(index: Int, limit: Int, status: String = "", resonanceType: String = "", query: String = ""): Future[Table[Data[CurationCandidate]]] =
    Api.request(s"/service/curation/candidates?index=$index&limit=$limit${renderParameter("status", status)}${renderParameter("resonanceType", resonanceType)}${renderParameter("query", query)}")
      .get
      .read[Table[Data[CurationCandidate]]]

  private def renderParameter(name: String, value: String): String = {
    val normalized = Option(value).map(_.trim).getOrElse("")
    if (normalized.isEmpty) ""
    else s"&$name=${encodeURIComponent(normalized)}"
  }
}
