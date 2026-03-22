package app.domain.shared

import app.domain.core.User
import kotlinx.serialization.Serializable

@Serializable
class Like(val user : User = User())