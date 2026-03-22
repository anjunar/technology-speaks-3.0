package app.domain.documents

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
import kotlinx.serialization.Transient
import org.w3c.fetch.RequestInit
import kotlin.time.Clock

@Serializable
data class Issue (
    @Serializable(with = PropertySerializer::class)
    override var id : Property<String>? = null,

    @Serializable(with = PropertySerializer::class)
    override val modified : Property<LocalDateTime> = Property(Clock.System.now().toLocalDateTime(TimeZone.UTC)),

    @Serializable(with = PropertySerializer::class)
    override val created : Property<LocalDateTime> = Property(Clock.System.now().toLocalDateTime(TimeZone.UTC)),

    @Serializable(with = PropertySerializer::class)
    var title : Property<String> = Property(""),

    @Serializable(with = PropertySerializer::class)
    override val user : Property<User>? = null,

    @Serializable(with = NodeSerializer::class)
    val editor: Property<EditorNode?> = Property(null),

    @SerialName($$"$links")
    @Serializable(with = ListPropertySerializer::class)
    override val links : ListProperty<Link> = ListProperty()
) : AbstractEntity, OwnerProvider {

    @Transient
    val editable = Property(false)

    companion object {

        suspend fun read(id : String) : Data<Issue> {
            return JsonClient.invoke<Data<Issue>>("/service/document/documents/document/$id/issues/issue")
        }

        suspend fun read(documentId : String, issueId : String) : Data<Issue> {
            return JsonClient.invoke<Data<Issue>>("/service/document/documents/document/$documentId/issues/issue/$issueId")
        }

        suspend fun list(index : Int, limit : Int, document : Document) : Table<Data<Issue>> {
            return JsonClient.invoke<Table<Data<Issue>>>("/service/document/documents/document/${document.id!!.get()}/issues?index=$index&limit=$limit&sort=created:desc")
        }

    }

}