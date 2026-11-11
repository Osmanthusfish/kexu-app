package com.schedule.app.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serial commits evaluate against the last successful value; a failed write cannot roll back a later edit. */
class CommitQueue<T>(initial: T) {
    private val mutex = Mutex()
    private var current = initial
    /** The UI can use loaded data while maintenance remains ordered ahead of later writes. */
    suspend fun initialize(load: suspend () -> T, ready: (T) -> Unit, maintain: suspend (T) -> Unit) =
        commit({ load() }) { _, next -> ready(next); maintain(next) }
    suspend fun <R> read(block: suspend (T) -> R): R = mutex.withLock { block(current) }
    suspend fun commit(write: suspend (T) -> T, publish: suspend (T, T) -> Unit) = mutex.withLock {
        val previous = current
        val next = write(previous)
        current = next
        publish(previous, next)
    }
}
