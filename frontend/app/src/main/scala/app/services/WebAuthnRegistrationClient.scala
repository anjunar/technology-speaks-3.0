package app.services

import app.support.Api

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js

object WebAuthnRegistrationClient {

  def register(
    email: String,
    nickName: String,
    optionsUrl: String = "/service/security/register/options",
    finishUrl: String = "/service/security/register/finish"
  ): Future[String] = {
    val request = js.Dynamic.literal(
      email = email,
      nickName = nickName
    )

    for {
      optionsJsonText <- Api.postText(optionsUrl, js.JSON.stringify(request))
      finishBody = js.JSON.stringify(
        js.Dynamic.literal(
          optionsJSON = js.JSON.parse(optionsJsonText),
          email = email,
          nickName = nickName
        )
      )
      result <- Api.postText(finishUrl, finishBody)
    } yield result
  }
}
