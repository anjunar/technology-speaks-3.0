package app.domain.curation

import jfx.core.meta.PackageClassLoader

object CurationRegistry {

  def init(): Unit = {
    val loader = PackageClassLoader("app.domain.curation")

    loader.register(() => new SourceRef(), classOf[SourceRef])
    loader.register(() => new DocumentTarget(), classOf[DocumentTarget])
    loader.register(() => new CurationDecision(), classOf[CurationDecision])
    loader.register(() => new CurationCandidate(), classOf[CurationCandidate])
    loader.register(() => new CurationCluster(), classOf[CurationCluster])
    loader.register(() => new CurationLink(), classOf[CurationLink])
    loader.register(() => new ClassifyCandidateRequest(), classOf[ClassifyCandidateRequest])
    loader.register(() => new AssignTargetRequest(), classOf[AssignTargetRequest])
    loader.register(() => new AddCandidateRequest(), classOf[AddCandidateRequest])
    loader.register(() => new WriteSummaryRequest(), classOf[WriteSummaryRequest])
    loader.register(() => new DecisionNoteRequest(), classOf[DecisionNoteRequest])
    loader.register(() => new CreateClusterRequest(), classOf[CreateClusterRequest])
  }
}
