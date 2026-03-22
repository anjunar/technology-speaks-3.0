package app.domain.security

import app.domain.core.AbstractLink
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("web-authn-register-options")
class WebAuthnRegisterLink(
    override val id: String,
    override val rel: String,
    override val url: String,
    override val method : String = "GET") : AbstractLink() {

    override val name : String = "Registrierrung mit WebAuthn"
    override val icon: String = "how_to_reg"

}