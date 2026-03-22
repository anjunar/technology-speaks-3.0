@file:Suppress("UnsafeCastFromDynamic")

package app.services

import jFx2.webauthn.SimpleWebAuthnBrowser.startRegistration
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.w3c.fetch.INCLUDE
import org.w3c.fetch.RequestCredentials
import org.w3c.fetch.RequestInit

object WebAuthnRegistrationClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Serializable
    data class RegisterOptionsRequest(
        val email: String,
        val nickName: String
    )

    suspend fun register(
        email: String,
        nickName: String,
        optionsUrl: String = "/service/security/register/options",
        finishUrl: String = "/service/security/register/finish"
    ): String {

        val optionsJsonText = postJsonText(
            url = optionsUrl,
            body = json.encodeToString(RegisterOptionsRequest.serializer(), RegisterOptionsRequest(email, nickName))
        )

        val optionsDyn = json.parseToJsonElement(optionsJsonText).toDynamic()

        val registrationResponse = startRegistration(
            js("{ optionsJSON: optionsDyn, nickName: nickName, email: email }")
        ).await()

        // 3) response ans backend schicken
        val finishBody = js("JSON.stringify({optionsJSON: registrationResponse, nickName: nickName, email: email})")
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
            credentials = RequestCredentials.INCLUDE
        )
    ).await()

    val text = resp.text().await()
    if (!resp.ok) error("HTTP ${resp.status} ${resp.statusText}: $text")
    return text
}

private fun JsonElement.toDynamic(): dynamic =
    js("JSON.parse")(this.toString())
