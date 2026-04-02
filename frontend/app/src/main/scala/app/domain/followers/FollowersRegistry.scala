package app.domain.followers

import jfx.core.meta.PackageClassLoader

object FollowersRegistry {
  
  def init(): Unit = {
    val loader = PackageClassLoader("app.domain.followers")
    
    loader.register(() => new RelationShip(), classOf[RelationShip])
    loader.register(() => new Group(), classOf[Group])
    loader.register(() => new GroupAssignmentRequest(), classOf[GroupAssignmentRequest])
  }
}
