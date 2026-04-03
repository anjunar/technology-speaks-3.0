package app.domain.curation

import app.domain.core.{AbstractEntity, Data, Table}
import app.support.Api
import app.support.Api.given
import jfx.core.state.{ListProperty, Property}

import scala.concurrent.Future
import scala.scalajs.js.URIUtils.encodeURIComponent

class CurationCluster extends AbstractEntity {
  val title: Property[String] = Property("")
  val summary: Property[String | Null] = Property(null)
  val status: Property[String] = Property(ClusterStatus.Offen)
  val target: Property[DocumentTarget | Null] = Property(null)
  val candidateIds: ListProperty[String] = ListProperty()
  val contradictionCount: Property[Int] = Property(0)
  val questionCount: Property[Int] = Property(0)
  val acceptedCount: Property[Int] = Property(0)
  val rejectedCount: Property[Int] = Property(0)

  def addCandidate(candidateId: String): Future[Data[CurationCluster]] =
    Api.request(s"/service/curation/clusters/${id.get}/add-candidate")
      .post(new AddCandidateRequest(candidateId))
      .read[Data[CurationCluster]]

  def writeSummary(value: String): Future[Data[CurationCluster]] =
    Api.request(s"/service/curation/clusters/${id.get}/write-summary")
      .post(new WriteSummaryRequest(value))
      .read[Data[CurationCluster]]

  def accept(note: String = ""): Future[Data[CurationCluster]] =
    Api.request(s"/service/curation/clusters/${id.get}/accept")
      .post(new DecisionNoteRequest(note))
      .read[Data[CurationCluster]]

  def defer(note: String = ""): Future[Data[CurationCluster]] =
    Api.request(s"/service/curation/clusters/${id.get}/defer")
      .post(new DecisionNoteRequest(note))
      .read[Data[CurationCluster]]

  def reject(note: String = ""): Future[Data[CurationCluster]] =
    Api.request(s"/service/curation/clusters/${id.get}/reject")
      .post(new DecisionNoteRequest(note))
      .read[Data[CurationCluster]]
}

object CurationCluster {
  def create(title: String): Future[Data[CurationCluster]] =
    Api.request("/service/curation/clusters")
      .post(new CreateClusterRequest(title))
      .read[Data[CurationCluster]]

  def list(index: Int, limit: Int, status: String = "", query: String = ""): Future[Table[Data[CurationCluster]]] =
    Api.request(s"/service/curation/clusters?index=$index&limit=$limit${renderParameter("status", status)}${renderParameter("query", query)}")
      .get
      .read[Table[Data[CurationCluster]]]

  private def renderParameter(name: String, value: String): String = {
    val normalized = Option(value).map(_.trim).getOrElse("")
    if (normalized.isEmpty) ""
    else s"&$name=${encodeURIComponent(normalized)}"
  }
}
