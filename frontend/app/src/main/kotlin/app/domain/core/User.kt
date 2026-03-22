package app.domain.core

import jFx2.client.JsonClient
import jFx2.state.ListProperty
import jFx2.state.ListPropertySerializer
import jFx2.state.Property
import jFx2.state.PropertySerializer
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock

@Serializable
data class User(
    @Serializable(with = PropertySerializer::class)
    override var id : Property<String>? = null,
    @Serializable(with = PropertySerializer::class)
    override val modified : Property<LocalDateTime> = Property(Clock.System.now().toLocalDateTime(TimeZone.UTC)),
    @Serializable(with = PropertySerializer::class)
    override val created : Property<LocalDateTime> = Property(Clock.System.now().toLocalDateTime(TimeZone.UTC)),
    @Serializable(with = PropertySerializer::class)
    val nickName: Property<String> = Property(""),
    @Serializable(with = PropertySerializer::class)
    val image : Property<Media?> = Property(null),
    @Serializable(with = PropertySerializer::class)
    var info: Property<UserInfo?> = Property(null),
    @Serializable(with = PropertySerializer::class)
    var address: Property<Address?> = Property(null),
    @Serializable(with = ListPropertySerializer::class)
    val emails: ListProperty<Email> = ListProperty(),
    @SerialName($$"$links")
    @Serializable(with = ListPropertySerializer::class)
    override val links : ListProperty<Link> = ListProperty()
)  : AbstractEntity {

    suspend fun save() : Data<User> {
        return JsonClient.post("/service/core/users/user", this)
    }

    suspend fun update() : Data<User> {
        return JsonClient.put("/service/core/users/user", this)
    }

    suspend fun delete() {
        JsonClient.delete("/service/core/users/user", this)
    }

    companion object {

        suspend fun read(id : String) : Data<User> {
            return JsonClient.invoke<Data<User>>("/service/core/users/user/$id")
        }

        suspend fun list(index : Int, limit : Int) : Table<Data<User>> {
            return JsonClient.invoke<Table<Data<User>>>("/service/core/users?index=$index&limit=$limit&sort=created:desc")
        }
    }
}
