package jFx2.router

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class RouteMatch(
    val route: Route,
    val fullPath: String,
    val params: Map<String, String> = emptyMap(),
    val id : Uuid? = Uuid.random()
)