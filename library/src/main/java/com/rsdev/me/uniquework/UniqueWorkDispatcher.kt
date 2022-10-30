package com.rsdev.me.uniquework

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.*
import java.lang.Runnable


/**
 * Wraps a given [CoroutineDispatcher] to additionally handle [UniqueWork] tags
 * whenever some work is dispatched.
 */
internal class UniqueWorkDispatcher(
    val delegate: CoroutineDispatcher,
    val container: UniqueWorkContainer,
) : CoroutineDispatcher() {

    override fun isDispatchNeeded(context: CoroutineContext) = delegate.isDispatchNeeded(context)

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        val workElement = context[UniqueWork]
        if (workElement != null) {
            val currentWork = container[workElement]

            if (currentWork == null || currentWork.shouldCancelFor(context)) {
                currentWork?.cancel(UniqueWorkCancellationException(workElement, isRestarted = true))

                container[workElement] = UniqueWorkData(workElement, context.job)
                context.job.invokeOnCompletion { reason ->
                    if (reason !is UniqueWorkCancellationException) {
                        container.remove(workElement)
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

internal class UniqueWorkData(val tag: UniqueWork, val job: Job) {

    /**
     * Check if this work should be cancelled for the given block.
     * @return true if the block runs in a different job hierarchy than this work.
     */
    fun shouldCancelFor(block: CoroutineContext): Boolean {
        val parentJob = job ?: return false
        val blockJob = block[Job] ?: return false
        return parentJob != blockJob && !parentJob.isIndirectParentOf(blockJob)
    }

    fun cancel(cause: UniqueWorkCancellationException) {
        job.cancel(cause)
    }

    private fun Job.isIndirectParentOf(child: Job): Boolean {
        return children.any { job ->
            job == child || job.isIndirectParentOf(child)
        }
    }
}