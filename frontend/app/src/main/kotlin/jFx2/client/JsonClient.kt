package jFx2.client

import app.domain.core.AbstractLink
import app.domain.core.Link
import app.domain.core.UsersLink
import app.domain.documents.DocumentsLink
import app.domain.followers.RelationShipLink
import app.domain.security.ConfirmLink
import app.domain.security.LogoutLink
import app.domain.security.PasswordLoginLink
import app.domain.security.PasswordRegisterLink
import app.domain.security.WebAuthnLoginLink
import app.domain.security.WebAuthnRegisterLink
import app.domain.timeline.PostsLink
import jFx2.encodeURIComponent
import jFx2.forms.ErrorResponse
import jFx2.router.navigate
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit

object JsonClient {

    suspend inline fun <reified O> invoke(url: String, requestInit: RequestInit = RequestInit()): O {
        val response = window.fetch(url, requestInit).await()

        if (!response.ok) {
            if (response.status == 403.toShort()) {
                val platformAvailable = js("PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable();")
                if (!platformAvailable) {
                    navigate("/security/login?redirect=${encodeURIComponent(window.location.pathname)}")
                } else {
                    navigate("/security/login/options?redirect=${encodeURIComponent(window.location.pathname)}")
                }
                throw RuntimeException("Error not Allowed")
            } else {
                if (response.status == 400.toShort()) {
                    val defaultJson = configure()

                    val responses = defaultJson.decodeFromString<List<ErrorResponse>>(response.text().await())

                    throw ErrorResponseException(responses)
                } else {
                    throw RuntimeException("Error ${response.status} ${response.statusText}")
                }
            }
        } else {
            val defaultJson = configure()

            return defaultJson.decodeFromString<O>(response.text().await())
        }
    }

    suspend inline fun <reified I, reified O> invoke(url: Link, entity : I): O {
        val headers = Headers()
        headers.set("Content-Type", "application/json")
        headers.set("Accept", "application/json")

        return invoke("/service" + url.url, RequestInit(
            method = url.method,
            headers = headers,
            body = Json.encodeToString(entity)
        ))
    }

    suspend inline fun <reified I, reified O> post(url: String, entity : I): O {
        val headers = Headers()
        headers.set("Content-Type", "application/json")
        headers.set("Accept", "application/json")

        return invoke(url, RequestInit(
            method = "POST",
            headers = headers,
            body = Json.encodeToString(entity)
        ))
    }

    suspend inline fun <reified I, reified O> put(url: String, entity : I): O {
        val headers = Headers()
        headers.set("Content-Type", "application/json")
        headers.set("Accept", "application/json")

        return invoke(url, RequestInit(
            method = "PUT",
            headers = headers,
            body = Json.encodeToString(entity)
        ))
    }

    suspend inline fun <reified I> delete(url: String, entity : I) {
        val headers = Headers()
        headers.set("Content-Type", "application/json")

        val requestInit = RequestInit(
            method = "DELETE",
            headers = headers,
            body = Json.encodeToString(entity)
        )

        val response = window.fetch(url, requestInit).await()

        if (!response.ok) throw RuntimeException("Error ${response.status} ${response.statusText}")

    }

    fun configure(): Json {
        val module = SerializersModule {
            polymorphic(AbstractLink::class) {
                subclass(WebAuthnLoginLink::class)
                subclass(WebAuthnRegisterLink::class)
                subclass(PasswordLoginLink::class)
                subclass(PasswordRegisterLink::class)
                subclass(ConfirmLink::class)
                subclass(UsersLink::class)
                subclass(LogoutLink::class)
                subclass(PostsLink::class)
                subclass(DocumentsLink::class)
                subclass(RelationShipLink::class)
            }
        }

        val defaultJson = Json {
            serializersModule = module
            encodeDefaults = true
            ignoreUnknownKeys = true
            explicitNulls = true
        }
        return defaultJson
    }
}