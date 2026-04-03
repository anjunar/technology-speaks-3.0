package app.services

import app.support.Api

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

object WebAuthnLoginClient {

  def login(
    email: String,
    optionsUrl: String = "/service/security/login/options",
    finishUrl: String = "/service/security/login/finish"
  ): Future[String] = {
    val request = js.Dynamic.literal(email = email)

    for {
      optionsJsonText <- Api.request(optionsUrl).post(js.JSON.stringify(request)).text
      optionsDyn = js.JSON.parse(optionsJsonText)
      authenticationResponse <- SimpleWebAuthnBrowser
        .startAuthentication(
          js.Dynamic.literal(
            optionsJSON = optionsDyn
          )
        )
        .toFuture
      finishBody = js.JSON.stringify(
        js.Dynamic.literal(
          optionsJSON = authenticationResponse,
          email = email
        )
      )
      result <- Api.request(finishUrl).post(finishBody).text
    } yield result
  }
}
