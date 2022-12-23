package com.rsdev.me.coroutinetasks

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.*
import java.lang.Runnable

/**
 * Wraps a given [CoroutineDispatcher] to additionally handle [Task] tags whenever some work is dispatched.
 */
internal class TaskDispatcher(
        val delegate: CoroutineDispatcher,
        val container: TaskContainer,
) : CoroutineDispatcher() {

    override fun isDispatchNeeded(context: CoroutineContext) = delegate.isDispatchNeeded(context)

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        val taskElement = context[Task]
        if (taskElement != null) {
            val currentWork = container[taskElement]

            if (currentWork == null || currentWork.shouldCancelFor(context)) {
                currentWork?.cancel(TaskCancellationException(taskElement, isRestarted = true))

                container[taskElement] = TaskData(taskElement, context.job)
                context.job.invokeOnCompletion { reason ->
                    if (reason !is TaskCancellationException) {
                        container.remove(taskElement)
                    }
                }
            } else {
                /*
                 * We are dispatching work within the unique job or it's children.
                 * Do not cancel anything and let the parent complete peacefully.
                 */
            }
        }
        delegate.dispatch(context, block)
    }
}

internal class TaskData(val task: Task, val job: Job) {

    /**
     * Check if this work should be cancelled for the given block.
     * @return true if the block runs in a different job hierarchy than this work.
     */
    fun shouldCancelFor(block: CoroutineContext): Boolean {
        val blockJob = block[Job] ?: return false
        return job != blockJob && !job.isIndirectParentOf(blockJob)
    }

    fun cancel(cause: TaskCancellationException) {
        job.cancel(cause)
    }

    private fun Job.isIndirectParentOf(child: Job): Boolean {
        return children.any { job ->
            job == child || job.isIndirectParentOf(child)
        }
    }
}