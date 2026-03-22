package app.domain.shared

import app.domain.core.Link
import app.domain.core.User
import jFx2.state.ListProperty
import jFx2.state.Property
import kotlinx.datetime.LocalDateTime

interface OwnerProvider {

    val modified : Property<LocalDateTime>

    val created : Property<LocalDateTime>

    val user : Property<User>?

    val links : ListProperty<Link>

}