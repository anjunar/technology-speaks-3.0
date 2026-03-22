package jFx2.core

interface ContextKey<T : Any>

class Ctx private constructor(
    private val map: MutableMap<ContextKey<*>, Any>
) {
    constructor() : this(HashMap())

    fun fork(): Ctx = Ctx(HashMap(map))

    fun <T : Any> set(key: ContextKey<T>, value: T) {
        map[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(key: ContextKey<T>): T =
        map[key] as? T ?: error("Missing context value for key: $key")
}

