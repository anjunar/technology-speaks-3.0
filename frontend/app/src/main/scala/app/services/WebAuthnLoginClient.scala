package app.services

import app.support.Api

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js

object WebAuthnLoginClient {

  def login(
    email: String,
    optionsUrl: String = "/service/security/login/options",
    finishUrl: String = "/service/security/login/finish"
  ): Future[String] = {
    val request = js.Dynamic.literal(email = email)

    for {
      optionsJsonText <- Api.postText(optionsUrl, js.JSON.stringify(request))
      finishBody = js.JSON.stringify(
        js.Dynamic.literal(
          optionsJSON = js.JSON.parse(optionsJsonText),
          email = email
        )
      )
      result <- Api.postText(finishUrl, finishBody)
    } yield result
  }
}
