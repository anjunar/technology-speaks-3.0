package app.services

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("@simplewebauthn/browser", JSImport.Namespace)
object SimpleWebAuthnBrowser extends js.Object {

  def startAuthentication(options: js.Any): js.Promise[js.Any] = js.native

  def startRegistration(options: js.Any): js.Promise[js.Any] = js.native
}
