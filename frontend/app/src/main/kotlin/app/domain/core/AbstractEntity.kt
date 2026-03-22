package app.domain.core

import jFx2.state.ListProperty
import jFx2.state.Property
import kotlinx.datetime.LocalDateTime

interface AbstractEntity {

    var id: Property<String>?

    val modified : Property<LocalDateTime>

    val created : Property<LocalDateTime>

    val links: ListProperty<Link>


}