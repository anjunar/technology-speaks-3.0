package app.domain.documents

import app.domain.core.Data
import app.domain.timeline.Post
import app.services.ApplicationService.Message

data class IssueCreated(val post: Data<Issue>) : Message