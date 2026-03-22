package app.domain.timeline

import app.domain.core.Data
import app.services.ApplicationService.Message

final class PostUpdated(val post: Data[Post]) extends Message
