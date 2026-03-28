package app.domain.security

import app.domain.core.Data
import app.domain.core.Link
import app.support.{AppJson, JsonModel}
import jfx.core.macros.typedProperty
import jfx.core.state.{ListProperty, PropertyAccess}
import org.scalajs.dom.{RequestInit, fetch}

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

class Account(
  val links: ListProperty[Link] = ListProperty()
) extends JsonModel[Account] {

  override def properties: js.Array[PropertyAccess[Account, ?]] =
    Account.properties
}

object Account {
  private given ExecutionContext = ExecutionContext.global

  val properties: js.Array[PropertyAccess[Account, ?]] = js.Array(
    typedProperty[Account, ListProperty[Link], Link](_.links)
  )

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
          AppJson.mapper.deserialize(js.JSON.parse(text).asInstanceOf[js.Dynamic]).asInstanceOf[Data[Account]]
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
