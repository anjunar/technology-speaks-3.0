package app.domain.security

import app.domain.core.AbstractLink
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@SerialName("logout-logout")
class LogoutLink(
    override val id: String,
    override val rel: String,
    override val url: String,
    override val method: String = "GET"
) : AbstractLink() {

    override val name: String = "Logout"
    override val icon: String = "logout"
}