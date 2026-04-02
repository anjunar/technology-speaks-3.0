package app.domain.security

import app.domain.core.Data
import app.domain.core.Link
import app.support.{AppJson, JsonModel}
import jfx.core.meta.Meta
import jfx.core.state.ListProperty
import org.scalajs.dom.{RequestInit, fetch}
import reflect.macros.ReflectMacros.reflectType

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

class Account(
  val links: ListProperty[Link] = ListProperty()
) extends JsonModel[Account] {


}

object Account {
  private given ExecutionContext = ExecutionContext.global



  def read(): Future[Data[Account]] =
    fetch(
      "/service/security/account",
      js.Dynamic.literal(
        method = "GET",
        credentials = "include",
        headers = js.Dictionary(
          "Accept" -> "application/json"
        )
      ).asInstanceOf[RequestInit]
    ).toFuture.flatMap { response =>
      if (response.ok) {
        response.text().toFuture.map { text =>
          AppJson.mapper.deserialize(js.JSON.parse(text), reflectType[Data[Account]]).asInstanceOf[Data[Account]]
        }
      } else {
        Future.successful(legacy())
      }
    }

  def legacy(): Data[Account] = {
    val account = new Account()
    account.links.addOne(Link("changePassword", "/security/account/password", "POST"))
    new Data(account)
  }
}
