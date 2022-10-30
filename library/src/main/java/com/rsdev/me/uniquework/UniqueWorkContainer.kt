package com.rsdev.me.uniquework

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * An object to store information of running coroutines with [UniqueWork] elements.
 * Work is only unique within a single container instance.
 * An instance can be created and added as element to any [CoroutineContext].
 * Alternatively it can also be passed directly to any dispatcher using [CoroutineDispatcher.asUniqueWorkDispatcher].
 */
class UniqueWorkContainer : AbstractCoroutineContextElement(Key) {

    private val map = ConcurrentHashMap<UniqueWork, UniqueWorkData>()

    internal operator fun get(key: UniqueWork) = map[key]
    internal operator fun set(key: UniqueWork, value: UniqueWorkData) = map.set(key, value)
    internal fun remove(key: UniqueWork) = map.remove(key)

    fun cancelWork(tag: Any): Boolean {
        val element = (tag as? UniqueWork) ?: UniqueWork(tag)
        return map.remove(element)?.also { it.cancel(UniqueWorkCancellationException(element, isRestarted = false)) } != null
    }

    companion object Key: CoroutineContext.Key<UniqueWorkContainer>
}

/**
 * The default global instance
 */
internal val DefaultUniqueWorkContainer by lazy { UniqueWorkContainer() }