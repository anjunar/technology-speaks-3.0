package app.domain.followers

import kotlinx.serialization.Serializable

@Serializable
data class GroupAssignmentRequest(
    val groupIds: List<String> = emptyList()
)

