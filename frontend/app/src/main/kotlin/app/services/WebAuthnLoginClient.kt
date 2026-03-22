@file:Suppress("UnsafeCastFromDynamic")

package app.services

import jFx2.webauthn.SimpleWebAuthnBrowser.startAuthentication
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.w3c.fetch.INCLUDE
import org.w3c.fetch.RequestCredentials
import org.w3c.fetch.RequestInit

object WebAuthnLoginClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Serializable
    data class LoginOptionsRequest(
        val email: String
    )

    suspend fun login(
        email: String,
        optionsUrl: String = "/service/security/login/options",
        finishUrl: String = "/service/security/login/finish"
    ): String {
        val optionsJsonText = postJsonText(
            url = optionsUrl,
            body = json.encodeToString(LoginOptionsRequest.serializer(), LoginOptionsRequest(email))
        )

        val optionsDyn = json.parseToJsonElement(optionsJsonText).toDynamic()

        val asseResp = startAuthentication(
            js("{ optionsJSON: optionsDyn }")
        ).await()

        val finishBody = js("JSON.stringify({optionsJSON : asseResp, email : email})")
        return postJsonText(
            url = finishUrl,
            body = finishBody.unsafeCast<String>()
        )
    }
}

private suspend fun postJsonText(url: String, body: String): String {
    val resp = window.fetch(
        url,
        RequestInit(
            method = "POST",
            headers = js("{ 'Content-Type': 'application/json' }"),
            body = body,
            credentials = RequestCredentials.INCLUDE // wichtig, falls du Session Cookies nutzt
        )
    ).await()

    val text = resp.text().await()
    if (!resp.ok) error("HTTP ${resp.status} ${resp.statusText}: $text")
    return text
}

private fun JsonElement.toDynamic(): dynamic =
    js("JSON.parse")(
        this.toString()
    )
