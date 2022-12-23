package com.rsdev.me.coroutinetasks

import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.*


/**
 * Wrap this dispatcher to handle [Task] tags. Provide a [container] to group tasks into.
 * Work is only considered unique within a single [TaskContainer]. When omitted, a default globally shared instance is used.
 */
fun CoroutineDispatcher.asTaskDispatcher(container: TaskContainer?): ContinuationInterceptor {
    if (this is TaskDispatcher && this.container == container) return this

    val actualDispatcher = if (this is TaskDispatcher) delegate else this
    val taskContainer = container ?: DefaultTaskContainer
    return TaskDispatcher(actualDispatcher, taskContainer)
}

/**
 * Wrap this dispatcher to also handle [Task] tags.
 *
 * This suspending function will inherit the [TaskContainer] from the current context, unlike it's non-suspending counterpart.
 * If the current context does not contain a container element, a globally shared default instance will be used.
 */
suspend fun CoroutineDispatcher.asTaskDispatcher() = asTaskDispatcher(currentCoroutineContext()[TaskContainer])
