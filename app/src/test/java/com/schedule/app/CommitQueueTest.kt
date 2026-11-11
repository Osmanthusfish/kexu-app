package com.schedule.app

import com.schedule.app.data.CommitQueue
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CommitQueueTest {
    @Test fun startupPublishesBeforeMaintenanceButLaterWritesWaitForIt() = runBlocking {
        val queue = CommitQueue(0)
        val ready = CompletableDeferred<Int>()
        val maintenance = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        val load = launch { queue.initialize({ 10 }, { ready.complete(it) }) { maintenance.await(); events.add("startup alarms") } }
        assertEquals(10, ready.await())
        assertTrue(load.isActive)
        val edit = launch { queue.commit({ events.add("save"); it + 1 }) { _, next -> events.add("alarms $next") } }
        yield()
        assertTrue(events.isEmpty())
        maintenance.complete(Unit)
        joinAll(load, edit)
        assertEquals(listOf("startup alarms", "save", "alarms 11"), events)
        assertEquals(11, queue.read { it })
    }
    @Test fun failedStartupDoesNotPublishEmptyDataAndQueueCanRecover() = runBlocking {
        val queue = CommitQueue(7)
        var ready = false
        assertTrue(runCatching { queue.initialize({ throw java.io.IOException("read failed") }, { ready = true }, {}) }.isFailure)
        assertFalse(ready)
        assertEquals(7, queue.read { it })
        queue.initialize({ 42 }, { ready = true }, {})
        assertTrue(ready)
        assertEquals(42, queue.read { it })
    }

    @Test fun queuedEditsUseLastSuccessfulValueAndPublishInOrder() = runBlocking {
        val queue = CommitQueue(0)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val published = mutableListOf<Int>()
        val first = launch { queue.commit({ entered.complete(Unit); release.await(); it + 1 }) { _, next -> published.add(next) } }
        entered.await()
        val second = launch { queue.commit({ it + 10 }) { _, next -> published.add(next) } }
        yield()
        assertTrue(published.isEmpty())
        release.complete(Unit)
        joinAll(first, second)
        assertEquals(listOf(1, 11), published)
        assertEquals(11, queue.read { it })
    }
    @Test fun failedWriteCannotPublishOrEraseLaterEdit() = runBlocking {
        val queue = CommitQueue(listOf("original"))
        var published = emptyList<String>()
        assertTrue(runCatching { queue.commit({ throw java.io.IOException("disk full") }) { _, next -> published = next } }.isFailure)
        assertTrue(published.isEmpty())
        queue.commit({ it + "later" }) { _, next -> published = next }
        assertEquals(listOf("original", "later"), published)
    }
    @Test fun sideEffectsFinishBeforeNextCommit() = runBlocking {
        val queue = CommitQueue(0)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        val first = launch { queue.commit({ 1 }) { _, _ -> entered.complete(Unit); release.await(); events.add("alarm1") } }
        entered.await()
        val second = launch { queue.commit({ events.add("save2"); 2 }) { _, _ -> events.add("alarm2") } }
        yield()
        assertTrue(events.isEmpty())
        release.complete(Unit)
        joinAll(first, second)
        assertEquals(listOf("alarm1", "save2", "alarm2"), events)
    }
}
