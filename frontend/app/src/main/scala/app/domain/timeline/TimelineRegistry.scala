package app.domain.timeline

import jfx.core.meta.PackageClassLoader
import app.domain.shared.{Like, FirstComment, SecondComment}

object TimelineRegistry {
  
  def init(): Unit = {
    val loader = PackageClassLoader("app.domain.timeline")
    
    loader.register(() => new Post())
    loader.register(() => new Like())
    loader.register(() => new FirstComment())
    loader.register(() => new SecondComment())
  }
}
