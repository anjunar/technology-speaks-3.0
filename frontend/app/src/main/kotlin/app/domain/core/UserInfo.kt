package app.domain.core

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
data class UserInfo(
    @Serializable(with = PropertySerializer::class)
    override var id : Property<String>? = null,
    @Serializable(with = PropertySerializer::class)
    override val modified : Property<LocalDateTime> = Property(Clock.System.now().toLocalDateTime(TimeZone.UTC)),
    @Serializable(with = PropertySerializer::class)
    override val created : Property<LocalDateTime> = Property(Clock.System.now().toLocalDateTime(TimeZone.UTC)),
    @Serializable(with = PropertySerializer::class)
    val firstName: Property<String> = Property(""),
    @Serializable(with = PropertySerializer::class)
    val lastName: Property<String> = Property(""),
    @Serializable(with = PropertySerializer::class)
    val birthDate: Property<String> = Property(""),
    @SerialName($$"$links")
    @Serializable(with = ListPropertySerializer::class)
    override val links : ListProperty<Link> = ListProperty()
)  : AbstractEntity