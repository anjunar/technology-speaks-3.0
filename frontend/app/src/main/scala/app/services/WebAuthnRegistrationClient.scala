package app.services

import app.support.Api

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

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
      optionsDyn = js.JSON.parse(optionsJsonText)
      registrationResponse <- SimpleWebAuthnBrowser
        .startRegistration(
          js.Dynamic.literal(
            optionsJSON = optionsDyn
          )
        )
        .toFuture
      finishBody = js.JSON.stringify(
        js.Dynamic.literal(
          optionsJSON = registrationResponse,
          email = email,
          nickName = nickName
        )
      )
      result <- Api.postText(finishUrl, finishBody)
    } yield result
  }
}
