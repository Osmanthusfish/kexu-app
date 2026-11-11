package com.schedule.app

import com.schedule.app.data.AtomicSnapshotFile
import com.schedule.app.data.Wk
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class StorageSafetyTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun store(file: File, fail: () -> Unit = {}) = AtomicSnapshotFile(file, {
        require(it.startsWith("valid:")) { "corrupt data" }
    }, fail)

    @Test fun invalidDatesAreRejectedWithoutThrowing() {
        listOf("2026-02-30", "2026-13-01", "2025-02-29", "", "2026-2-03").forEach {
            assertNull(Wk.parseDateOrNull(it))
        }
        assertEquals("2024-02-29", Wk.parseDateOrNull("2024-02-29").toString())
        assertEquals("2026-09-07", Wk.mondayOf(Wk.parseDateOrNull("2026-09-09")!!).toString())
    }

    @Test fun missingOrCorruptMainUsesValidBackup() {
        val file = File(temporary.root, "data.json")
        val storage = store(file)
        storage.backup.writeText("valid:previous")
        assertEquals("valid:previous" to true, storage.readValid())
        file.writeText("corrupted")
        assertEquals("valid:previous" to true, storage.readValid())
        storage.write("valid:new")
        assertEquals("valid:previous", storage.backup.readText())
        assertEquals("valid:new", file.readText())
    }

    @Test fun failedWritePreservesReadableCurrentAndBackup() {
        val file = File(temporary.root, "data.json")
        file.writeText("valid:current")
        val storage = store(file) { throw IOException("simulated write failure") }
        assertThrows(IOException::class.java) { storage.write("valid:new") }
        assertEquals("valid:current", file.readText())
        assertEquals("valid:current", storage.backup.readText())
    }

    @Test fun invalidIncomingDataDoesNotTouchCurrentOrBackup() {
        val file = File(temporary.root, "data.json")
        file.writeText("valid:current")
        val storage = store(file)
        storage.backup.writeText("valid:older")
        assertThrows(IllegalArgumentException::class.java) { storage.write("wrong") }
        assertEquals("valid:current", file.readText())
        assertEquals("valid:older", storage.backup.readText())
    }

    @Test fun fullyCorruptFilesArePreservedUntilExplicitRecovery() {
        val file = File(temporary.root, "data.json")
        file.writeText("corrupt current")
        val storage = store(file)
        storage.backup.writeText("corrupt previous")
        assertThrows(IOException::class.java) { storage.write("valid:empty") }
        assertEquals("corrupt current", file.readText())
        assertEquals("corrupt previous", storage.backup.readText())
    }
}
