package app.domain.security

import app.domain.core.AbstractLink
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("password-register-register")
class PasswordRegisterLink(
    override val id: String,
    override val rel: String,
    override val url: String,
    override val method : String = "GET") : AbstractLink() {

    override val name : String = "Registrierung mit Passwort"
    override val icon: String = "app_registration"

}