package jFx2.router

import jFx2.core.Component
import jFx2.core.capabilities.NodeScope

data class Route (
    val path: String,
    val factory: (suspend context(NodeScope) (Map<String, String>) -> Component<*>)? = null,
    val children: List<Route> = emptyList()
)