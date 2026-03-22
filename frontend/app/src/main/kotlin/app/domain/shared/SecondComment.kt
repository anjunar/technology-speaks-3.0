package app.domain.shared

import app.domain.core.AbstractEntity
import app.domain.core.Data
import app.domain.core.Link
import app.domain.core.User
import app.domain.documents.Document
import app.domain.timeline.Post
import jFx2.client.JsonClient
import jFx2.forms.editor.EditorNode
import jFx2.forms.editor.NodeSerializer
import jFx2.state.ListProperty
import jFx2.state.ListPropertySerializer
import jFx2.state.Property
import jFx2.state.PropertySerializer
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import kotlin.time.Clock

@Serializable
data class SecondComment(
    @Serializable(with = PropertySerializer::class)
    override var id: Property<String>? = null,
    @Serializable(with = PropertySerializer::class)
    override val modified : Property<LocalDateTime> = Property(Clock.System.now().toLocalDateTime(TimeZone.UTC)),
    @Serializable(with = PropertySerializer::class)
    override val created : Property<LocalDateTime> = Property(Clock.System.now().toLocalDateTime(TimeZone.UTC)),
    @Serializable(with = PropertySerializer::class)
    override var user: Property<User>? = null,
    @Serializable(with = NodeSerializer::class)
    val editor: Property<EditorNode?> = Property(null),
    @Serializable(with = ListPropertySerializer::class)
    val likes : ListProperty<Like> = ListProperty(),
    @SerialName($$"$links")
    @Serializable(with = ListPropertySerializer::class)
    override val links : ListProperty<Link> = ListProperty()
) : AbstractEntity, OwnerProvider {

    @Transient
    val editable = Property(false)

    suspend fun save(document : Document) : Data<FirstComment> {
        return JsonClient.post("/service/document/documents/document/issues/issue/${document.id!!.get()}/comment", this)
    }

    suspend fun save(post : Post) : Data<FirstComment> {
        return JsonClient.post("/service//timeline/posts/post/${post.id!!.get()}/comment", this)
    }

    suspend fun update(document : Document) : Data<FirstComment> {
        return JsonClient.put("/service/document/documents/document/issues/issue/${document.id!!.get()}/comment", this)
    }

    suspend fun update(post : Post) : Data<FirstComment> {
        return JsonClient.put("/service//timeline/posts/post/${post.id!!.get()}/comment", this)
    }


}

