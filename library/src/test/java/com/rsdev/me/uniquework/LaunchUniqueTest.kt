package com.rsdev.me.uniquework

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
internal class LaunchUniqueTest {

    private val TAG = "test"

    @Test
    fun cancelWithJob() = runTest {
        var done1 = false
        var done2 = false
        withContext(UniqueWorkContainer()) {
            launchUnique(UniqueWork(TAG)) {
                try {
                    delay(50L)
                    fail("Job should not complete")
                } catch (e: CancellationException) {
                    assertTrue(e is UniqueWorkCancellationException && e.isRestarted) {
                        "Should cancel with UniqueWorkCancellationException marked as relaunched"
                    }
                }
                done1 = true
            }
            delay(10L)
            launchUnique(UniqueWork(TAG)) {
                delay(10L)
                done2 = true
            }
        }
        assertTrue(done1 && done2) { "Test not done" }
    }

    @Test
    fun cancelManually() = runTest {
        var done = false
        withContext(UniqueWorkContainer()) {
            launchUnique(UniqueWork(TAG)) {
                try {
                    delay(50L)
                    fail("Job should not complete")
                } catch (e: CancellationException) {
                    assertTrue(e is UniqueWorkCancellationException && !e.isRestarted) {
                        "Should cancel with UniqueWorkCancellationException marked as manual"
                    }
                }
                done = true
            }
            delay(25L)
            runCurrent()
            cancelUnique(TAG)
        }
        advanceUntilIdle()
        assertTrue(done) { "Test not done" }
    }

    @Test
    fun notCancelledByChildren() = runTest {
        var done = false
        launchUnique(UniqueWork(TAG)) {
            try {
                coroutineScope<Unit> {
                    launch { delay(25L) }
                    launch(UniqueWork(TAG)) { delay(25L) }
                    awaitAll(
                        async { delay(25L) },
                        async(UniqueWork(TAG)) { delay(25L) }
                    )
                    launchUnique(UniqueWork(TAG)) {
                        delay(25L)
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