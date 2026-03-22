package app.domain

import app.domain.core.AbstractLink
import app.domain.core.User
import jFx2.client.JsonClient
import jFx2.state.ListProperty
import jFx2.state.ListPropertySerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Application(
    val user: User = User(),
    @SerialName($$"$links")
    @Serializable(with = ListPropertySerializer::class)
    val links: ListProperty<AbstractLink> = ListProperty()
) {

    companion object {
        suspend fun read() : Application {
            return JsonClient.invoke("/service")
        }
    }

}