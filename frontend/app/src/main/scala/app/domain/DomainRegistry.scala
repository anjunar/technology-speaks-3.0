package app.domain

import app.domain.core.{CoreRegistry, Link, UsersLink}
import app.domain.timeline.{PostsLink, TimelineRegistry}
import app.domain.documents.{DocumentsLink, DocumentsRegistry}
import app.domain.followers.{FollowersRegistry, RelationShipLink}
import app.domain.security.*
import app.domain.shared.SharedRegistry
import jfx.core.meta.PackageClassLoader
import jfx.domain.{Media, Thumbnail}
import jfx.form.ErrorResponse

object DomainRegistry {

  def init(): Unit = {
    val loader = PackageClassLoader("app.domain")

    loader.register(() => new Application(), classOf[Application])
    
    loader.register(() => new Link(), classOf[Link])
    loader.register(() => new UsersLink(), classOf[UsersLink])
    loader.register(() => new PostsLink(), classOf[PostsLink])
    loader.register(() => new DocumentsLink(), classOf[DocumentsLink])
    loader.register(() => new RelationShipLink(), classOf[RelationShipLink])
    loader.register(() => new PasswordLoginLink(), classOf[PasswordLoginLink])
    loader.register(() => new PasswordRegisterLink(), classOf[PasswordRegisterLink])
    loader.register(() => new WebAuthnLoginLink(), classOf[WebAuthnLoginLink])
    loader.register(() => new WebAuthnRegisterLink(), classOf[WebAuthnRegisterLink])
    loader.register(() => new LogoutLink(), classOf[LogoutLink])
    loader.register(() => new ConfirmLink(), classOf[ConfirmLink])

    loader.register(() => new Thumbnail(), classOf[Thumbnail])
    loader.register(() => new Media(), classOf[Media])
    loader.register(() => new ErrorResponse(), classOf[ErrorResponse])

    CoreRegistry.init()
    TimelineRegistry.init()
    DocumentsRegistry.init()
    FollowersRegistry.init()
    SecurityRegistry.init()
    SharedRegistry.init()
  }
}
