package com.rsdev.me.uniquework

import kotlin.coroutines.*
import kotlinx.coroutines.*

/**
 * An element that can be added to a CoroutineContext to mark it as Unique. If the same unique work tag is
 * detected in a different context, the first job will be cancelled.
 *
 * Unique tags are only considered unique within the same [UniqueWorkContainer]. A container can be added to any context
 * to limit the scope of unique tags. When omitted from the context, a default globally shared instance will be used.
 *
 * This mechanism is only works with supported Dispatchers. A suitable dispatchers can be obtained with
 * [CoroutineDispatcher.asUniqueWorkDispatcher] or will be installed automatically by [CoroutineScope.launchUnique]
 */
data class UniqueWork(val tag: Any): AbstractCoroutineContextElement(Key) {

    companion object Key: CoroutineContext.Key<UniqueWork>
}
