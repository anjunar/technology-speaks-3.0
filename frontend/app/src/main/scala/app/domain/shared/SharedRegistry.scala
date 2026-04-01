package app.domain.shared

import jfx.core.meta.PackageClassLoader

object SharedRegistry {
  
  def init(): Unit = {
    val loader = PackageClassLoader("app.domain.shared")
    
    loader.register(() => new Like())
    loader.register(() => new FirstComment())
    loader.register(() => new SecondComment())
  }
}
