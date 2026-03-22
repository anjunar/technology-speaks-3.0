package app.domain.followers

import app.domain.core.AbstractEntity
import app.domain.core.Address
import app.domain.core.Data
import app.domain.core.Email
import app.domain.core.Link
import app.domain.core.Media
import app.domain.core.Table
import app.domain.core.User
import app.domain.core.UserInfo
import jFx2.client.JsonClient
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
data class Group(
    @Serializable(with = PropertySerializer::class)
    override var id : Property<String>? = null,
    @Serializable(with = PropertySerializer::class)
    val name : Property<String> = Property(""),
    @Serializable(with = PropertySerializer::class)
    override val modified : Property<LocalDateTime> = Property(Clock.System.now().toLocalDateTime(TimeZone.UTC)),
    @Serializable(with = PropertySerializer::class)
    override val created : Property<LocalDateTime> = Property(Clock.System.now().toLocalDateTime(TimeZone.UTC)),
    @Serializable(with = ListPropertySerializer::class)
    val users : ListProperty<User> = ListProperty(),
    @SerialName($$"$links")
    @Serializable(with = ListPropertySerializer::class)
    override val links : ListProperty<Link> = ListProperty()
) : AbstractEntity {

    @Transient
    val editable = Property(false)

    suspend fun save() : Data<Group> {
        return JsonClient.post("/service/followers/groups/groups", this)
    }

    suspend fun update() : Data<Group> {
        return JsonClient.put("/service/followers/groups/groups", this)
    }

    suspend fun delete() {
        JsonClient.delete("/service/followers/groups/groups", this)
    }

    companion object {

        suspend fun read(id : String) : Data<Group> {
            return JsonClient.invoke<Data<Group>>("/service/followers/groups/groups/$id")
        }

        suspend fun list(index : Int, limit : Int) : Table<Data<Group>> {
            return JsonClient.invoke<Table<Data<Group>>>("/service/followers/groups?index=$index&limit=$limit&sort=created:desc")
        }
    }


}
