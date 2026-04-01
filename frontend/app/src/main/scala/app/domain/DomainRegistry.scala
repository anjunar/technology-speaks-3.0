package app.domain

import app.domain.core.CoreRegistry
import app.domain.timeline.TimelineRegistry
import app.domain.documents.DocumentsRegistry
import app.domain.followers.FollowersRegistry
import app.domain.security.SecurityRegistry
import app.domain.shared.SharedRegistry

object DomainRegistry {
  
  def init(): Unit = {
    CoreRegistry.init()
    TimelineRegistry.init()
    DocumentsRegistry.init()
    FollowersRegistry.init()
    SecurityRegistry.init()
    SharedRegistry.init()
  }
}
