package app.support

import app.domain.Application
import app.domain.core.*
import app.domain.documents.*
import app.domain.followers.*
import app.domain.security.*
import app.domain.shared.*
import app.domain.timeline.*
import jfx.domain.{Media, Thumbnail}
import jfx.form.ErrorResponse
import jfx.json.JsonMapper
import jfx.json.JsonRegistry

import scala.scalajs.js
import scala.scalajs.js.Dynamic
import scala.scalajs.js.JSConverters.*

class AppJsonRegistry extends JsonRegistry {

  private val mapper = new JsonMapper(this)

  valueFactories += classOf[Schema].getName -> (() => new Schema())
  valueDeserializers += classOf[Schema].getName -> deserializeSchema
  valueSerializers += classOf[Schema] -> serializeSchema

  override val classes: js.Map[String, () => Any] = js.Map(
    "Application" -> (() => new Application()),
    "Data" -> (() => new Data[js.Any]()),
    "Table" -> (() => new Table[js.Any]()),
    "Schema" -> (() => new Schema()),
    "Property" -> (() => new SchemaProperty()),
    "Link" -> (() => new Link()),
    "ManagedProperty" -> (() => new ManagedProperty()),
    "Address" -> (() => new Address()),
    "Email" -> (() => new Email()),
    "EMail" -> (() => new Email()),
    "ErrorResponse" -> (() => new ErrorResponse()),
    "Account" -> (() => new Account()),
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
    "account-read" -> (() => new AccountLink()),
    "password-change-change" -> (() => new AccountLink()),
    "PasswordChange" -> (() => new PasswordChange()),
    "CreatePassword" -> (() => new CreatePassword()),
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

  private def deserializeSchema(raw: js.Any): Any = {
    val dynamic = raw.asInstanceOf[Dynamic]
    val entries = js.Dictionary[SchemaProperty]()

    js.Object.keys(dynamic.asInstanceOf[js.Object]).foreach { key =>
      if (key != "@type") {
        val property = mapper.deserialize(dynamic.selectDynamic(key)).asInstanceOf[SchemaProperty]
        property.name = key
        entries.update(key, property)
      }
    }

    new Schema(entries)
  }

  private def serializeSchema(value: Any): js.Any = {
    val schema = value.asInstanceOf[Schema]
    val out = js.Dictionary[js.Any]()

    schema.entries.foreach { (key, property) =>
      out.update(key, mapper.serialize(property))
    }

    out.update("@type", "Schema")
    out.asInstanceOf[js.Any]
  }
}
