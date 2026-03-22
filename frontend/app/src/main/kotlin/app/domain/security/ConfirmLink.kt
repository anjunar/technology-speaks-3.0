package app.domain.security

import app.domain.core.AbstractLink
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@SerialName("confirm-confirm")
class ConfirmLink(
    override val id: String,
    override val rel: String,
    override val url: String,
    override val method: String = "GET"
) : AbstractLink() {

    override val name: String = "Bestätigen"
    override val icon: String = "approval"
}