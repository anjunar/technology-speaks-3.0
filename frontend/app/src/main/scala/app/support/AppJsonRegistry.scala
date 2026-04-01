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
import jfx.core.meta.Meta

import scala.scalajs.js
import scala.scalajs.js.Dynamic
import scala.scalajs.js.JSConverters.*

class AppJsonRegistry {

  val mapper = new JsonMapper()

  // Registriere alle Model-Klassen im ClassLoader via Meta
  Meta[Application](() => new Application())
  Meta[Data[js.Any]](() => new Data[js.Any]())
  Meta[Table[js.Any]](() => new Table[js.Any]())
  Meta[Schema](() => new Schema())
  Meta[SchemaProperty](() => new SchemaProperty())
  Meta[Link](() => new Link())
  Meta[ManagedProperty](() => new ManagedProperty())
  Meta[Address](() => new Address())
  Meta[EMail](() => new EMail())
  Meta[ErrorResponse](() => new ErrorResponse())
  Meta[Account](() => new Account())
  Meta[Media](() => new Media())
  Meta[Thumbnail](() => new Thumbnail())
  Meta[User](() => new User())
  Meta[UserInfo](() => new UserInfo())
  Meta[UsersLink](() => new UsersLink())
  Meta[Document](() => new Document())
  Meta[DocumentsLink](() => new DocumentsLink())
  Meta[Issue](() => new Issue())
  Meta[Group](() => new Group())
  Meta[GroupAssignmentRequest](() => new GroupAssignmentRequest())
  Meta[RelationShip](() => new RelationShip())
  Meta[RelationShipLink](() => new RelationShipLink())
  Meta[PasswordLogin](() => new PasswordLogin())
  Meta[PasswordLoginLink](() => new PasswordLoginLink())
  Meta[AccountLink](() => new AccountLink())
  Meta[PasswordChange](() => new PasswordChange())
  Meta[CreatePassword](() => new CreatePassword())
  Meta[PasswordRegister](() => new PasswordRegister())
  Meta[PasswordRegisterLink](() => new PasswordRegisterLink())
  Meta[WebAuthnLogin](() => new WebAuthnLogin())
  Meta[WebAuthnLoginLink](() => new WebAuthnLoginLink())
  Meta[WebAuthnRegister](() => new WebAuthnRegister())
  Meta[WebAuthnRegisterLink](() => new WebAuthnRegisterLink())
  Meta[ConfirmLink](() => new ConfirmLink())
  Meta[LogoutLink](() => new LogoutLink())
  Meta[Like](() => new Like())
  Meta[FirstComment](() => new FirstComment())
  Meta[SecondComment](() => new SecondComment())
  Meta[Post](() => new Post())
  Meta[PostsLink](() => new PostsLink())
  Meta[JsonResponse](() => new JsonResponse())

  private def deserializeSchema(raw: js.Any): Any = {
    val dynamic = raw.asInstanceOf[Dynamic]
    val entries = js.Dictionary[SchemaProperty]()

    js.Object.keys(dynamic.asInstanceOf[js.Object]).foreach { key =>
      if (key != "@type") {
        val property = mapper.deserialize(dynamic.selectDynamic(key), SchemaProperty.meta).asInstanceOf[SchemaProperty]
        property.name = key
        entries.update(key, property)
      }
    }

    entries.asInstanceOf[Any]
  }

  private def serializeSchema(value: Any): js.Any = {
    val schema = value.asInstanceOf[Schema]
    mapper.serialize(schema)
  }
}
