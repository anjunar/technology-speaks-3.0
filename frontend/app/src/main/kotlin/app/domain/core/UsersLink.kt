package app.domain.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@SerialName("users-list")
class UsersLink(
    override val id: String,
    override val rel: String,
    override val url: String,
    override val method: String = "GET"
) : AbstractLink() {

    override val name: String = "Benutzer"
    override val icon: String = "diversity_3"

}