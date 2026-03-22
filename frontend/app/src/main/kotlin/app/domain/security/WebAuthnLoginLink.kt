package app.domain.security

import app.domain.core.AbstractLink
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@SerialName("web-authn-login-options")
class WebAuthnLoginLink(
    override val id: String,
    override val rel: String,
    override val url: String,
    override val method : String = "GET") : AbstractLink() {

    override val name : String = "Login mit WebAuthn"
    override val icon: String = "fingerprint"

}