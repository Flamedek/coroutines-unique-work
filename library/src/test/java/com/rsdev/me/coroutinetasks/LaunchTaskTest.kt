package com.rsdev.me.coroutinetasks

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
internal class LaunchTaskTest {

    private val TAG = "test"

    @Test
    fun cancelWithJob() = runTest {
        var done1 = false
        var done2 = false
        withContext(TaskContainer()) {
            launchTask(Task(TAG)) {
                try {
                    delay(50L)
                    fail("Job should not complete")
                } catch (e: CancellationException) {
                    assertTrue(e is TaskCancellationException && e.isRestarted) {
                        "Should cancel with UniqueWorkCancellationException marked as restarted"
                    }
                }
                done1 = true
            }
            delay(10L)
            launchTask(Task(TAG)) {
                delay(10L)
                done2 = true
            }
        }
        assertTrue(done1 && done2) { "Test not done" }
    }

    @Test
    fun cancelManually() = runTest {
        var done = false
        withContext(TaskContainer()) {
            launchTask(Task(TAG)) {
                try {
                    delay(50L)
                    fail("Job should not complete")
                } catch (e: CancellationException) {
                    assertTrue(e is TaskCancellationException && !e.isRestarted) {
                        "Should cancel with UniqueWorkCancellationException marked as manual"
                    }
                }
                done = true
            }
            delay(10L)
            cancelTask(TAG)
        }
        assertTrue(done) { "Test not done" }
    }

    @Test
    fun notCancelledByChildren() = runTest {
        var done = false
        launchTask(Task(TAG)) {
            try {
                coroutineScope<Unit> {
                    launch { delay(10L) }
                    launch(Task(TAG)) { delay(10L) }
                    awaitAll(
                        async { delay(10L) },
                        async(Task(TAG)) { delay(10L) }
                    )
                    launchTask(Task(TAG)) {
                        delay(10L)
                    }
                }
                done = true
            } catch (e: CancellationException) {
                fail("Job should not be cancelled")
            }
        }.join()
        assertTrue(done) { "Test not done" }
    }
}