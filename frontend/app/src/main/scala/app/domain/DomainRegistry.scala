package app.domain

import app.domain.core.CoreRegistry
import app.domain.timeline.TimelineRegistry
import app.domain.documents.DocumentsRegistry
import app.domain.followers.FollowersRegistry
import app.domain.security.SecurityRegistry
import app.domain.shared.SharedRegistry
import jfx.core.meta.PackageClassLoader

object DomainRegistry {
  
  def init(): Unit = {
    val loader = PackageClassLoader("app.domain")

    loader.register(() => new Application())

    CoreRegistry.init()
    TimelineRegistry.init()
    DocumentsRegistry.init()
    FollowersRegistry.init()
    SecurityRegistry.init()
    SharedRegistry.init()
  }
}
