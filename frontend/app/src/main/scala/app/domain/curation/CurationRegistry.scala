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
  }
}
