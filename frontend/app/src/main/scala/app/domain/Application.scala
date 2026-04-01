package app.domain

import app.domain.core.{AbstractLink, User, UsersLink}
import app.domain.documents.DocumentsLink
import app.domain.followers.RelationShipLink
import app.domain.security.*
import app.domain.timeline.PostsLink
import app.support.{Api, JsonModel}
import app.support.Api.given
import com.anjunar.scala.enterprise.macros.validation.JsonName
import jfx.core.meta.Meta
import jfx.core.state.ListProperty

import scala.annotation.meta.field
import scala.concurrent.Future

class Application(
  var user: User = new User(),
  @(JsonName @field)("$links")
  val links: ListProperty[AbstractLink] = ListProperty()
) extends JsonModel[Application] {

  override def meta: Meta[Application] = Application.meta

}

object Application {

  val subClasses: Seq[Meta[?]] = Seq(
    AccountLink.meta,
    UsersLink.meta,
    ConfirmLink.meta,
    DocumentsLink.meta,
    LogoutLink.meta,
    PostsLink.meta,
    PasswordLoginLink.meta,
    PasswordRegisterLink.meta,
    WebAuthnLoginLink.meta,
    WebAuthnRegisterLink.meta,
    RelationShipLink.meta,
  )

  subClasses.foreach(println)

  val meta: Meta[Application] = Meta[Application](() => new Application())

  def read(): Future[Application] = Api.get("/service").map(raw => Api.deserialize(raw, Application.meta))

}
