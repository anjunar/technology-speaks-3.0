package app.domain.shared

import app.domain.core.AbstractEntity
import app.domain.core.Data
import app.domain.core.Link
import app.domain.core.Table
import app.domain.core.User
import app.domain.documents.Document
import app.domain.documents.Issue
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
import kotlin.time.Clock

@Serializable
data class FirstComment(
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
    @Serializable(with = ListPropertySerializer::class)
    val comments : ListProperty<SecondComment> = ListProperty(),
    @SerialName($$"$links")
    @Serializable(with = ListPropertySerializer::class)
    override val links : ListProperty<Link> = ListProperty()
) : AbstractEntity, OwnerProvider {

    @Transient
    val editable = Property(false)

    suspend fun save(entity : AbstractEntity) : Data<FirstComment> {
        when(entity) {
            is Document -> {
                return JsonClient.post("/service/document/documents/document/issues/issue/${entity.id!!.get()}/comment", this)
            }
            is Post -> {
                return JsonClient.post("/service//timeline/posts/post/${entity.id!!.get()}/comment", this)
            }
            else -> throw Exception("Unknown document type")
        }

    }

    suspend fun update(entity : AbstractEntity) : Data<FirstComment> {
        when(entity) {
            is Document -> {
                return JsonClient.put("/service/document/documents/document/issues/issue/${entity.id!!.get()}/comment", this)
            }
            is Post -> {
                return JsonClient.put("/service//timeline/posts/post/${entity.id!!.get()}/comment", this)
            }
            else -> throw Exception("Unknown document type")
        }
    }

    suspend fun delete(entity : AbstractEntity) {
        when(entity) {
            is Document -> {
                JsonClient.delete("/service/document/documents/document/issues/issue/${entity.id!!.get()}/comment", this)
            }
            is Post -> {
                JsonClient.delete("/service//timeline/posts/post/${entity.id!!.get()}/comment", this)
            }
            else -> throw Exception("Unknown document type")
        }
    }

    companion object {
        suspend fun list(index : Int, limit : Int, post : Post) : Table<Data<FirstComment>> {
            return JsonClient.invoke<Table<Data<FirstComment>>>("/service/timeline/posts/post/${post.id!!.get()}/comments?index=$index&limit=$limit&sort=created:desc")
        }
        suspend fun list(index : Int, limit : Int, issue : Issue) : Table<Data<FirstComment>> {
            return JsonClient.invoke<Table<Data<FirstComment>>>("/service/document/documents/document/issues/issue/${issue.id!!.get()}/comments?index=$index&limit=$limit&sort=created:desc")
        }
    }

}


