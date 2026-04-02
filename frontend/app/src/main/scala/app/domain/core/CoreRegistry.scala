package app.domain.core

import jfx.core.meta.PackageClassLoader
import jfx.domain.Media

object CoreRegistry {
  
  def init(): Unit = {
    val loader = PackageClassLoader("app.domain.core")
    
    loader.register(() => new User(), classOf[User])
    loader.register(() => new UserInfo(), classOf[UserInfo])
    loader.register(() => new Address(), classOf[Address])
    loader.register(() => new EMail(), classOf[EMail])
    loader.register(() => new Media(), classOf[Media])
    loader.register(() => new ManagedProperty(), classOf[ManagedProperty])

    loader.register(() => new Table[Any](), classOf[Table[Any]])
    loader.register(() => new Data[Any](), classOf[Data[Any]])

    loader.register(() => new Schema(), classOf[Schema])
    loader.register(() => new SchemaProperty(), classOf[SchemaProperty])
  }
}
