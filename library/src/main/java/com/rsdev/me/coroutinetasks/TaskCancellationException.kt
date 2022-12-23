package com.rsdev.me.coroutinetasks

import kotlin.coroutines.cancellation.CancellationException

/**
 * Type of [CancellationException] which is used when a Job is cancelled as
 * the result of the same Task being ran in a different Job.
 */
class TaskCancellationException(
        val task: Task,
        val isRestarted: Boolean,
) : CancellationException(buildExceptionMessage(task.tag, isRestarted)) {

    inline val tag get() = task.tag

    companion object {
        @JvmStatic
        private fun buildExceptionMessage(tag: Any, isRestarted: Boolean): String {
            // Avoid potentially expensive toString operations.
            val tagString = tag.takeIf { it is String || it is Number }?.let { " '$it'" }.orEmpty()

            return if (isRestarted) {
                "Task$tagString was started in a different context."
            } else {
                "Task$tagString was explicitly cancelled."
            }
        }
    }
}
