package com.rsdev.me.uniquework

import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.*

/**
 * Launch a new coroutine and installs any prerequisites needed to manage unique work tags. The context passed to this function
 * must contain an [UniqueWork] element to mark it's unique tag. Previously launched jobs with the same unique tag will be cancelled.
 *
 * To scope uniqueness to a part of your application, a [UniqueWorkContainer] can be installed in the current context
 * or may be passed to this function. If omitted, a default globally shared storage will be used.
 *
 * This function takes care of wrapping any current Dispatcher with a UniqueWorkDispatcher which will check for uniqueness.
 * @throws IllegalArgumentException if [context] does not include a [UniqueWork]
 */
fun CoroutineScope.launchUnique(
    context: CoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit,
): Job {
    requireNotNull(context[UniqueWork]) { "context passed to launchUnique should contain UniqueWork." }

    val combinedContext = coroutineContext + context
    val currentDispatcher = combinedContext.currentDispatcherOrDefault()

    val container = combinedContext[UniqueWorkContainer] ?: (currentDispatcher as? UniqueWorkDispatcher)?.container
    val newDispatcher = currentDispatcher.asUniqueWorkDispatcher(container)

    val newContext = if (currentDispatcher != newDispatcher) {
        context + newDispatcher
    } else {
        context
    }
    return launch(newContext, start, block)
}

/**
 * Cancel any job running with the given [UniqueWork] tag. This will be scoped to the [UniqueWorkContainer] installed
 * in this context or resorts to the global default container if none is found.
 * @return true if a job was cancelled by this invocation.
 */
fun CoroutineScope.cancelUnique(tag: Any): Boolean {
    val dispatcher = coroutineContext[ContinuationInterceptor]
    val contextContainer = coroutineContext[UniqueWorkContainer] ?: (dispatcher as? UniqueWorkDispatcher)?.container
    val actualContainer = contextContainer ?: DefaultUniqueWorkContainer
    return actualContainer.cancelWork(tag)
}

/**
 * Mark this scope as a root container for unique work tag. This is equivalent to `this + UniqueWorkContainer()`
 */
fun CoroutineScope.asUniqueWorkContainer() = this + UniqueWorkContainer()

/**
 * Wrap this dispatcher to also handle [UniqueWork] tags.
 * @param container an instance of a container, tags are considered unique only within that container instance.
 * If null, a default globally shared instance is used.
 */
fun CoroutineDispatcher.asUniqueWorkDispatcher(container: UniqueWorkContainer?): ContinuationInterceptor {
    if (this is UniqueWorkDispatcher && this.container == container) return this

    val workDispatcher = if (this is UniqueWorkDispatcher) delegate else this
    val workContainer = container ?: DefaultUniqueWorkContainer
    return UniqueWorkDispatcher(workDispatcher, workContainer)
}

/**
 * Wrap this dispatcher to also handle [UniqueWork] tags.
 *
 * This suspending function will inherit the [UniqueWorkContainer] from the current context, unlike it's non-suspending counterpart.
 * If the current context does not contain a container element, a globally shared default instance will be used.
 */
suspend fun CoroutineDispatcher.asUniqueWorkDispatcher() =
    asUniqueWorkDispatcher(currentCoroutineContext()[UniqueWorkContainer])


private fun CoroutineContext.currentDispatcherOrDefault(): CoroutineDispatcher {
    return (get(ContinuationInterceptor) ?: Dispatchers.Default) as? CoroutineDispatcher ?: error(
        "ContinuationInterceptor in this context must be a CoroutineDispatcher"
    )
}