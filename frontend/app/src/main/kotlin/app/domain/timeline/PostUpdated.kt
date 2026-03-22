package app.domain.timeline

import app.domain.core.Data
import app.services.ApplicationService.Message

data class PostUpdated(val post: Data<Post>) : Message