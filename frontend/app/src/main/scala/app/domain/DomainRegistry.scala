package app.domain

import app.domain.core.{CoreRegistry, Link, UsersLink}
import app.domain.timeline.{PostsLink, TimelineRegistry}
import app.domain.documents.{DocumentsLink, DocumentsRegistry}
import app.domain.followers.{FollowersRegistry, RelationShipLink}
import app.domain.security._
import app.domain.shared.SharedRegistry
import jfx.core.meta.PackageClassLoader

object DomainRegistry {

  def init(): Unit = {
    val loader = PackageClassLoader("app.domain")

    loader.register(() => new Application())
    
    // Register all Link types for polymorphic deserialization
    loader.register(() => new Link())
    loader.register(() => new UsersLink())
    loader.register(() => new PostsLink())
    loader.register(() => new DocumentsLink())
    loader.register(() => new RelationShipLink())
    loader.register(() => new PasswordLoginLink())
    loader.register(() => new PasswordRegisterLink())
    loader.register(() => new WebAuthnLoginLink())
    loader.register(() => new WebAuthnRegisterLink())
    loader.register(() => new LogoutLink())
    loader.register(() => new AccountLink())
    loader.register(() => new ConfirmLink())

    CoreRegistry.init()
    TimelineRegistry.init()
    DocumentsRegistry.init()
    FollowersRegistry.init()
    SecurityRegistry.init()
    SharedRegistry.init()
  }
}
