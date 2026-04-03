package app.domain.security

import app.domain.core.Data
import app.domain.core.Link
import app.support.Api
import jfx.core.meta.Meta
import jfx.core.state.ListProperty

import scala.concurrent.{ExecutionContext, Future}

class Account(
  val links: ListProperty[Link] = ListProperty()
)

object Account {
  private given ExecutionContext = ExecutionContext.global



  def read(): Future[Data[Account]] =
    Api.request("/service/security/account").get.read[Data[Account]].recover { case _ => legacy() }

  def legacy(): Data[Account] = {
    val account = new Account()
    account.links.addOne(Link("changePassword", "/security/account/password", "POST"))
    new Data(account)
  }
}
