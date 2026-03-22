@file:Suppress("UnsafeCastFromDynamic")

package jFx2.webauthn

import kotlin.js.Promise

@JsModule("@simplewebauthn/browser")
@JsNonModule
external object SimpleWebAuthnBrowser {

    @JsName("startAuthentication")
    fun startAuthentication(options: dynamic): Promise<dynamic>

    @JsName("startRegistration")
    fun startRegistration(options: dynamic): Promise<dynamic>

    @JsName("browserSupportsWebAuthn")
    fun browserSupportsWebAuthn(): Boolean

    @JsName("platformAuthenticatorIsAvailable")
    fun platformAuthenticatorIsAvailable(): Promise<Boolean>
}
