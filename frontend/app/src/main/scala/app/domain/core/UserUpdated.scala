package app.domain.core

import app.services.ApplicationService.Message

final class UserUpdated(val user: Data[User]) extends Message
