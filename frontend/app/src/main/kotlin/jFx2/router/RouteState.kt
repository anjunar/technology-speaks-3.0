package jFx2.router

data class RouterState(
    val path: String,
    val matches: List<RouteMatch>
)