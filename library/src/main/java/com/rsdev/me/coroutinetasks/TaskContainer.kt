package com.rsdev.me.coroutinetasks

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * An object to store information of active [Task] elements.
 * Tasks are only unique and mutually exclusive within a single container instance.
 * An instance can be created and added as element to any [CoroutineContext].
 * Alternatively it can also be passed directly to any dispatcher using [CoroutineDispatcher.asTaskDispatcher].
 */
class TaskContainer : AbstractCoroutineContextElement(Key) {

    private val map = ConcurrentHashMap<Task, TaskData>()

    internal operator fun get(key: Task) = map[key]
    internal operator fun set(key: Task, value: TaskData) = map.set(key, value)
    internal fun remove(key: Task) = map.remove(key)

    fun cancelTask(tag: Any): Boolean {
        val task = (tag as? Task) ?: Task(tag)
        return map.remove(task)?.also { it.cancel(TaskCancellationException(task, isRestarted = false)) } != null
    }

    companion object Key: CoroutineContext.Key<TaskContainer>
}

/**
 * Make this scope a new root container for tasks. This is equivalent to `this + TaskContainer()`
 */
fun CoroutineScope.asTaskContainer() = this + TaskContainer()

/**
 * The default global instance
 */
internal val DefaultTaskContainer by lazy { TaskContainer() }