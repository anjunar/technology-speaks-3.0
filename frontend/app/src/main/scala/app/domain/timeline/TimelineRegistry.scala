package app.domain.timeline

import jfx.core.meta.PackageClassLoader
import app.domain.shared.{Like, FirstComment, SecondComment}

object TimelineRegistry {
  
  def init(): Unit = {
    val loader = PackageClassLoader("app.domain.timeline")
    
    loader.register(() => new Post(), classOf[Post])
    loader.register(() => new Like(), classOf[Like])
    loader.register(() => new FirstComment(), classOf[FirstComment])
    loader.register(() => new SecondComment(), classOf[SecondComment])
  }
}
