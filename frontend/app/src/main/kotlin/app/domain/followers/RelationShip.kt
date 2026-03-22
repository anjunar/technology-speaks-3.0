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
import kotlin.time.Clock

@Serializable
class RelationShip(
    @Serializable(with = PropertySerializer::class)
    override var id : Property<String>? = null,
    @Serializable(with = PropertySerializer::class)
    override val modified : Property<LocalDateTime> = Property(Clock.System.now().toLocalDateTime(TimeZone.UTC)),
    @Serializable(with = PropertySerializer::class)
    override val created : Property<LocalDateTime> = Property(Clock.System.now().toLocalDateTime(TimeZone.UTC)),
    @Serializable(with = PropertySerializer::class)
    val follower : Property<User> = Property(User()),
    @Serializable(with = ListPropertySerializer::class)
    val users : ListProperty<User> = ListProperty(),
    @Serializable(with = ListPropertySerializer::class)
    val groups: ListProperty<Group> = ListProperty(),
    @SerialName($$"$links")
    @Serializable(with = ListPropertySerializer::class)
    override val links : ListProperty<Link> = ListProperty()
) : AbstractEntity {

    suspend fun save() : Data<RelationShip> {
        return JsonClient.post("/service/followers/relationships/relationship", this)
    }

    suspend fun update() : Data<RelationShip> {
        return JsonClient.put("/service/followers/relationships/relationship", this)
    }

    suspend fun delete() {
        JsonClient.delete("/service/followers/relationships/relationship", this)
    }

    companion object {

        suspend fun read(id : String) : Data<RelationShip> {
            return JsonClient.invoke<Data<RelationShip>>("/service/followers/relationships/relationship/$id")
        }

        suspend fun list(index : Int, limit : Int) : Table<Data<RelationShip>> {
            return JsonClient.invoke<Table<Data<RelationShip>>>("/service/followers/relationships?index=$index&limit=$limit&sort=created:desc")
        }
    }


}