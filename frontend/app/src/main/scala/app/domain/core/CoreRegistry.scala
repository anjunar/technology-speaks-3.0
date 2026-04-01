package app.domain.core

import jfx.core.meta.PackageClassLoader
import jfx.domain.Media

object CoreRegistry {
  
  def init(): Unit = {
    val loader = PackageClassLoader("app.domain.core")
    
    loader.register(() => new User())
    loader.register(() => new UserInfo())
    loader.register(() => new Address())
    loader.register(() => new EMail())
    loader.register(() => new Media())
    loader.register(() => new ManagedProperty())
  }
}
