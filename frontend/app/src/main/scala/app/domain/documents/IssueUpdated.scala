package app.domain.documents

import app.domain.core.Data
import app.services.ApplicationService.Message

final class IssueUpdated(val post: Data[Issue]) extends Message
