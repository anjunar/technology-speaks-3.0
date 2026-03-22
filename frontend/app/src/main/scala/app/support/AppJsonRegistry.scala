package app.support

import app.domain.Application
import app.domain.core.*
import app.domain.documents.*
import app.domain.followers.*
import app.domain.security.*
import app.domain.shared.*
import app.domain.timeline.*
import jfx.domain.{Media, Thumbnail}
import jfx.json.JsonRegistry

import scala.scalajs.js

class AppJsonRegistry extends JsonRegistry {

  override val classes: js.Map[String, () => Any] = js.Map(
    "Application" -> (() => new Application()),
    "Data" -> (() => new Data[js.Any]()),
    "Table" -> (() => new Table[js.Any]()),
    "Link" -> (() => new Link()),
    "Address" -> (() => new Address()),
    "Email" -> (() => new Email()),
    "Media" -> (() => new Media()),
    "Thumbnail" -> (() => new Thumbnail()),
    "User" -> (() => new User()),
    "UserInfo" -> (() => new UserInfo()),
    "users-list" -> (() => new UsersLink()),
    "Document" -> (() => new Document()),
    "document-root" -> (() => new DocumentsLink()),
    "Issue" -> (() => new Issue()),
    "Group" -> (() => new Group()),
    "GroupAssignmentRequest" -> (() => new GroupAssignmentRequest()),
    "RelationShip" -> (() => new RelationShip()),
    "followers-list" -> (() => new RelationShipLink()),
    "PasswordLogin" -> (() => new PasswordLogin()),
    "password-login-login" -> (() => new PasswordLoginLink()),
    "PasswordRegister" -> (() => new PasswordRegister()),
    "password-register-register" -> (() => new PasswordRegisterLink()),
    "WebAuthnLogin" -> (() => new WebAuthnLogin()),
    "web-authn-login-options" -> (() => new WebAuthnLoginLink()),
    "WebAuthnRegister" -> (() => new WebAuthnRegister()),
    "web-authn-register-options" -> (() => new WebAuthnRegisterLink()),
    "confirm-confirm" -> (() => new ConfirmLink()),
    "logout-logout" -> (() => new LogoutLink()),
    "Like" -> (() => new Like()),
    "FirstComment" -> (() => new FirstComment()),
    "SecondComment" -> (() => new SecondComment()),
    "Post" -> (() => new Post()),
    "posts-list" -> (() => new PostsLink()),
    "JsonResponse" -> (() => new JsonResponse())
  )
}
