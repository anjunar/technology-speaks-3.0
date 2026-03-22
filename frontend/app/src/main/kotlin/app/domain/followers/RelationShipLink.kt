package app.domain.followers

import app.domain.core.AbstractLink
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@SerialName("followers-list")
class RelationShipLink(
    override val id: String,
    override val rel: String,
    override val url: String,
    override val method: String = "GET"
) : AbstractLink() {

    override val name: String = "Followers"
    override val icon: String = "1k_plus"

}