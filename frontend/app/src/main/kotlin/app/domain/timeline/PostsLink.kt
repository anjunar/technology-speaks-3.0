package app.domain.timeline

import app.domain.core.AbstractLink
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("posts-list")
class PostsLink(
    override val id: String,
    override val rel: String,
    override val url: String,
    override val method : String = "GET") : AbstractLink() {

    override val name : String = "Posts"
    override val icon: String = "timeline"

}