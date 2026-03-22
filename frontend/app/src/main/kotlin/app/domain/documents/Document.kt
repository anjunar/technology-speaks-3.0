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
data class Document (
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

    suspend fun save() : Data<Document> {
        return JsonClient.post("/service/document/documents/document", this)
    }

    suspend fun update() : Data<Document> {
        return JsonClient.put("/service/document/documents/document", this)
    }

    suspend fun delete() {
        JsonClient.delete("/service/document/documents/document", this)
    }

    companion object {

        suspend fun root() : Data<Document> {
            return JsonClient.invoke<Data<Document>>( "/service/document/documents/document/root", RequestInit("POST"))
        }

        suspend fun list(index : Int, limit : Int) : Table<Data<Document>> {
            return JsonClient.invoke<Table<Data<Document>>>("/service/document/documents?index=$index&limit=$limit&sort=created:desc")
        }

    }

}