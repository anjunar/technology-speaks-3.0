@file:OptIn(ExperimentalUuidApi::class)

package jFx2.router

import kotlin.uuid.ExperimentalUuidApi

private fun normalize(path: String): String {
    if (path.isBlank()) return "/"
    var p = path.trim()
    if (!p.startsWith("/")) p = "/$p"
    // remove trailing slash except root
    if (p.length > 1 && p.endsWith("/")) p = p.dropLast(1)
    return p
}

private fun join(parent: String, child: String): String {
    val p = normalize(parent)
    val c = child.trim()
    if (c.isBlank() || c == "/") return p

    // IMPORTANT: treat child as relative when we are already inside a parent route
    val effectiveChild = if (p != "/" && c.startsWith("/")) c.drop(1) else c

    return normalize(
        if (p == "/") "/$effectiveChild".replace("//", "/")
        else "$p/$effectiveChild"
    )
}

private fun splitSegments(path: String): List<String> =
    normalize(path).trim('/').let { if (it.isBlank()) emptyList() else it.split('/') }

private fun matchPattern(pattern: String, actual: String): Map<String, String>? {
    val pSeg = splitSegments(pattern)
    val aSeg = splitSegments(actual)

    // wildcard support: "/foo/*"
    val wildcardIndex = pSeg.indexOf("*")
    if (wildcardIndex >= 0) {
        val prefix = pSeg.take(wildcardIndex)
        if (aSeg.size < prefix.size) return null
        val params = LinkedHashMap<String, String>()
        for (i in prefix.indices) {
            val ps = prefix[i]
            val asg = aSeg[i]
            if (ps.startsWith(":")) params[ps.drop(1)] = asg
            else if (ps != asg) return null
        }
        params["*"] = aSeg.drop(prefix.size).joinToString("/")
        return params
    }

    if (pSeg.size != aSeg.size) return null

    val params = LinkedHashMap<String, String>()
    for (i in pSeg.indices) {
        val ps = pSeg[i]
        val asg = aSeg[i]
        if (ps.startsWith(":")) params[ps.drop(1)] = asg
        else if (ps != asg) return null
    }
    return params
}

/**
 * Returns the best (deepest) match-chain for `path`, or empty if none.
 */
fun resolveRoutes(routes: List<Route>, path: String): RouterState {
    val target = normalize(path)

    fun dfs(parentFull: String, route: Route): List<RouteMatch>? {
        val full = join(parentFull, route.path)

        // For nested routing we want prefix matching for intermediate routes:
        // - if route has children: allow it to match as prefix ("/" matches everything, "/app" matches "/app/..")
        // - but also allow exact match (leaf)
        val routeFull = normalize(full)

        val routeSegments = splitSegments(routeFull)
        val targetSegments = splitSegments(target)

        // if route path is "/" it always prefix-matches
        val isRoot = routeFull == "/"

        val prefixOk = if (isRoot) true else {
            if (targetSegments.size < routeSegments.size) false
            else {
                var ok = true
                for (i in routeSegments.indices) {
                    val ps = routeSegments[i]
                    val ts = targetSegments[i]
                    if (ps == "*") break
                    if (ps.startsWith(":")) continue
                    if (ps != ts) {
                        ok = false
                        break
                    }
                }
                ok
            }
        }

        if (!prefixOk) return null

        // params only on exact component match patterns
        // (optional) allow :param in segments for prefix checks too
        val paramsForThis = matchPattern(routeFull, target) ?: emptyMap()

        // If exact match: return [this]
        if (matchPattern(routeFull, target) != null) {
            return listOf(RouteMatch(route, routeFull, paramsForThis))
        }

        // Try children for deeper match
        for (child in route.children) {
            val childChain = dfs(routeFull, child)
            if (childChain != null) {
                return listOf(RouteMatch(route, routeFull, emptyMap())) + childChain
            }
        }

        // If no child matched, but current route is "/" and target != "/":
        // allow "/" alone to be the match chain (acts as layout/root)
        if (routeFull == "/" && route.children.isNotEmpty()) {
            return listOf(RouteMatch(route, routeFull, emptyMap()))
        }

        // leaf mismatch
        return null
    }

    // choose the deepest chain among all top-level routes
    val chains = routes.mapNotNull { dfs("/", it) }
    val best = chains.maxByOrNull { it.size } ?: emptyList()

    return RouterState(path = target, matches = best)
}
