package app.domain.documents

import jfx.core.meta.PackageClassLoader

object DocumentsRegistry {
  
  def init(): Unit = {
    val loader = PackageClassLoader("app.domain.documents")
    
    loader.register(() => new Document())
    loader.register(() => new Issue())
  }
}
