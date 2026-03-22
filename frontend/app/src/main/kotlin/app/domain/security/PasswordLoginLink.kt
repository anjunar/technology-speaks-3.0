package app.domain.security

import app.domain.core.AbstractLink
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@SerialName("password-login-login")
class PasswordLoginLink(
    override val id: String,
    override val rel: String,
    override val url: String,
    override val method: String = "GET"
) : AbstractLink() {

    override val name: String = "Login mit Passwort"
    override val icon: String = "login"

}