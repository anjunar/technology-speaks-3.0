package app.domain.security

import jFx2.client.JsonClient
import jFx2.client.JsonResponse
import jFx2.state.Property
import jFx2.state.PropertySerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class PasswordRegister(
    @Serializable(with = PropertySerializer::class)
    val email : Property<String> = Property(""),
    @Serializable(with = PropertySerializer::class)
    val nickName : Property<String> = Property(""),
    @Serializable(with = PropertySerializer::class)
    val password : Property<String> = Property("")
) {

    suspend fun save() : JsonResponse {
        return JsonClient.post("/service/security/register", this)
    }

}