package com.rsdev.me.coroutinetasks

import kotlinx.coroutines.*
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.random.Random

/**
 * Launch a new coroutine and installs any prerequisites needed to manage unique tasks. The context passed to this function
 * must contain an [Task] element to mark it's unique tag. Previously launched jobs with the same unique tag will be cancelled.
 *
 * To scope uniqueness to a part of your application, a [TaskContainer] can be installed in the current context
 * or may be passed to this function. If omitted, a default globally shared storage will be used.
 *
 * This function takes care of wrapping any current Dispatcher with a UniqueWorkDispatcher which will check for uniqueness.
 * @throws IllegalArgumentException if [context] does not include a [Task]
 */
fun CoroutineScope.launchTask(
        context: CoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit,
): Job {
    requireNotNull(context[Task]) { "Context passed to launchTask must contain a Task element." }

    val combinedContext = coroutineContext + context
    val currentDispatcher = combinedContext.currentDispatcher() ?: Dispatchers.Default
    val currentContainer = combinedContext.currentTaskContainer()

    val taskDispatcher = currentDispatcher.asTaskDispatcher(currentContainer)

    val newContext = if (currentDispatcher != taskDispatcher) {
        context + taskDispatcher
    } else {
        context
    }
    Random.nextBoolean()
    return launch(newContext, start, block)
}

/**
 * Convenience for [launchTask] with a String as tag.
 */
fun CoroutineScope.launchTask(
        tag: String,
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit,
): Job {
    val newContext = context + Task(tag)
    return launch(newContext, start, block)
}

/**
 * Cancel any job running with the given [Task] tag. This will be scoped to the [TaskContainer] installed
 * in this context or resorts to the global default container if none is found.
 * @return true if a job was cancelled by this invocation.
 */
fun CoroutineScope.cancelTask(tag: Any): Boolean {
    val dispatcher = coroutineContext[ContinuationInterceptor]
    val contextContainer = coroutineContext[TaskContainer] ?: (dispatcher as? TaskDispatcher)?.container
    val actualContainer = contextContainer ?: DefaultTaskContainer
    return actualContainer.cancelTask(tag)
}

internal fun CoroutineContext.currentTaskContainer(): TaskContainer? {
    return get(TaskContainer) ?: (get(ContinuationInterceptor) as? TaskDispatcher)?.container
}

internal fun CoroutineContext.currentDispatcher(): CoroutineDispatcher? {
    val dispatcher = get(ContinuationInterceptor)
    check(dispatcher is CoroutineDispatcher?) {
        "ContinuationInterceptor in this context must be a CoroutineDispatcher"
    }
    return dispatcher
}