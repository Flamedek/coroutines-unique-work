package com.rsdev.me.uniquework

import kotlin.coroutines.cancellation.CancellationException

/**
 * Type of [CancellationException] which is used when a Job is cancelled because it's
 * associated [UniqueWork] is cancelled or dispatched in a different job.
 */
class UniqueWorkCancellationException(
    val element: UniqueWork,
    val isRestarted: Boolean,
) : CancellationException(buildExceptionMessage(element.tag, isRestarted)) {
    val tag get() = element.tag
}

private fun buildExceptionMessage(tag: Any, isRestarted: Boolean): String {
    // Avoid potentially expensive toString operations.
    val tagString = tag.takeIf { it is String || it is Number }?.let { " '$it'" }.orEmpty()

    return if (isRestarted) {
        "New job was launched with the same unique work tag$tagString."
    } else {
        "Unique work$tagString was explicitly cancelled."
    }
}
