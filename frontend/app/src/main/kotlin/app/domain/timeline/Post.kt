package app.domain.timeline

import app.domain.core.AbstractEntity
import app.domain.core.Data
import app.domain.core.Link
import app.domain.core.Table
import app.domain.core.User
import app.domain.shared.Like
import app.domain.shared.OwnerProvider
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
import org.w3c.fetch.RequestInit
import kotlin.time.Clock

@Serializable
data class Post (
    @Serializable(with = PropertySerializer::class)
    override var id : Property<String>? = null,
    @Serializable(with = PropertySerializer::class)
    override val modified : Property<LocalDateTime> = Property(Clock.System.now().toLocalDateTime(TimeZone.UTC)),
    @Serializable(with = PropertySerializer::class)
    override val created : Property<LocalDateTime> = Property(Clock.System.now().toLocalDateTime(TimeZone.UTC)),
    @Serializable(with = PropertySerializer::class)
    override val user : Property<User>? = null,
    @Serializable(with = NodeSerializer::class)
    val editor: Property<EditorNode?> = Property(null),
    @Serializable(with = ListPropertySerializer::class)
    val likes : ListProperty<Like> = ListProperty(),
    @SerialName($$"$links")
    @Serializable(with = ListPropertySerializer::class)
    override val links : ListProperty<Link> = ListProperty()
) : AbstractEntity, OwnerProvider {
    constructor() : this(
        id = null,
        user = null,
        editor = Property(null),
        likes = ListProperty()
    )

    suspend fun save() : Data<Post> {
        return JsonClient.post("/service/timeline/posts/post", this)
    }

    suspend fun update() : Data<Post> {
        return JsonClient.put("/service/timeline/posts/post", this)
    }

    suspend fun delete() {
        JsonClient.delete("/service/timeline/posts/post", this)
    }

    companion object {

        suspend fun read(id : String) : Data<Post> {
            return JsonClient.invoke<Data<Post>>("/service/timeline/posts/post/$id")
        }

        suspend fun list(index : Int, limit : Int) : Table<Data<Post>> {
            return JsonClient.invoke<Table<Data<Post>>>("/service/timeline/posts?index=$index&limit=$limit&sort=created:desc")
        }

    }
}