package com.schedule.app

import com.schedule.app.data.BackupCodec
import com.schedule.app.data.BackupPayload
import com.schedule.app.data.Store
import com.schedule.app.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.json.JSONObject
import java.io.File

class BackupTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun sample() = BackupCodec.emptyData().copy(
        semesters = listOf(Semester("sem", "旧学期", "2026-09-07", listOf(Course("c", "高等数学", "101", "王老师", "A101", 1, 1, 2, "1-16单")))),
        reminders = listOf(Reminder("r", "准备上课", 1, 8, 0, 10, "备注", true, 1)),
        countdowns = listOf(Countdown("event", "考试", "2026-12-20")),
        todos = listOf(Todo("todo", "作业", "2026-11-09", "2026-11-11", false)),
        bgImageUri = Store.WALLPAPER_MARKER, widgetBgImageUri = Store.WIDGET_WALLPAPER_MARKER,
        fontScale = 1.2f, blockOpacity = 0.75f, homeSemesterId = "sem", theme = 6, customAccent = -16711936
    )

    @Test fun currentBackupRoundTripsAllDataAndExactImageBytes() {
        val image = byteArrayOf(1, 2, 3, 4, 5)
        val widget = byteArrayOf(10, 20, 30)
        val result = BackupCodec.decode(BackupCodec.encode(BackupPayload(sample(), image, widget)))
        assertEquals(sample(), result.data)
        assertArrayEquals(image, result.wallpaper)
        assertArrayEquals(widget, result.widgetWallpaper)
    }

    @Test fun legacyV18FieldsRemainCompatible() {
        val legacy = """{"semesters":[{"id":"old","title":"上学期","start":"2026-09-07","courses":[]}],"reminders":[],"countdowns":[],"todos":[],"bg":"","theme":0,"pattern":0}"""
        val d = BackupCodec.decode(legacy).data
        assertEquals("old", d.semesters.single().id)
        assertTrue(d.semesters.single().showSaturday)
        assertEquals(-1, d.semesters.single().periodsPerDay)
        assertEquals(12, d.periodsPerDay)
        assertEquals(1f, d.blockOpacity, 0f)
        assertEquals(d, BackupCodec.decode(BackupCodec.encode(BackupPayload(d, null, null))).data)
    }

    @Test fun malformedOrUnrelatedFilesAreRejected() {
        listOf("", "{", "{}", "[]", """{"semesters":"wrong"}""", """{"semesters":[],"schemaVersion":999}""").forEach {
            assertThrows(Exception::class.java) { BackupCodec.decode(it) }
        }
    }

    @Test fun invalidDataTypesDatesAndDuplicateIdsAreRejected() {
        val valid = BackupCodec.encode(BackupPayload(sample(), null, null))
        val date = JSONObject(valid).apply { getJSONArray("semesters").getJSONObject(0).put("start", "2026-02-30") }
        assertThrows(Exception::class.java) { BackupCodec.decode(date.toString()) }
        val hour = JSONObject(valid).apply { getJSONArray("reminders").getJSONObject(0).put("hour", 25) }
        assertThrows(Exception::class.java) { BackupCodec.decode(hour.toString()) }
        val type = JSONObject(valid).apply { put("periodsPerDay", "twelve") }
        assertThrows(Exception::class.java) { BackupCodec.decode(type.toString()) }
        assertThrows(Exception::class.java) { BackupCodec.encode(BackupPayload(sample().copy(todos = listOf(sample().todos[0], sample().todos[0])), null, null)) }
    }

    @Test fun parsingAndRejectedImportNeverChangeImagesOrCurrentFile() {
        val store = Store(temporary.root)
        val originalImage = byteArrayOf(1, 2, 3)
        val file = File(temporary.root, "wallpaper.img").apply { writeBytes(originalImage) }
        store.save(sample())
        val originalJson = File(temporary.root, "data.json").readText()
        store.parse(BackupCodec.encode(BackupPayload(sample(), byteArrayOf(9, 9), null)))
        assertArrayEquals(originalImage, file.readBytes())
        assertThrows(Exception::class.java) { store.importBackup("{broken", sample()) }
        assertEquals(originalJson, File(temporary.root, "data.json").readText())
        assertFalse(store.hasImportSnapshot())
        assertArrayEquals(originalImage, file.readBytes())
    }

    @Test fun corruptAndMissingMainRestorePreviousData() {
        val store = Store(temporary.root)
        store.save(sample())
        store.save(sample().copy(theme = 2))
        val main = File(temporary.root, "data.json")
        main.writeText("broken")
        assertEquals(sample(), store.load())
        assertTrue(main.delete())
        assertEquals(sample(), Store(temporary.root).load())
    }

    @Test fun successfulImportCanRestorePreviousDataAndWallpaper() {
        val store = Store(temporary.root)
        val oldImage = byteArrayOf(1, 3, 5)
        File(temporary.root, "wallpaper.img").writeBytes(oldImage)
        store.save(sample())
        val next = sample().copy(todos = emptyList(), theme = 2)
        val imported = store.importBackup(BackupCodec.encode(BackupPayload(next, byteArrayOf(8, 9), null)), sample())
        assertEquals(next, imported)
        assertEquals(next, store.load())
        assertArrayEquals(byteArrayOf(8, 9), File(temporary.root, "wallpaper.img").readBytes())
        assertTrue(store.hasImportSnapshot())
        assertEquals(sample(), store.restoreBeforeImport())
        assertArrayEquals(oldImage, File(temporary.root, "wallpaper.img").readBytes())
        assertEquals(sample(), store.load())
    }

    @Test fun wallpaperCommitIsRecoverableAndDerivedFileCannotOverwriteSnapshot() {
        val store = Store(temporary.root)
        val data = sample()
        store.saveWallpaper(data, byteArrayOf(1, 2), false)
        store.saveWallpaper(data, byteArrayOf(3, 4), false)
        assertArrayEquals(byteArrayOf(3, 4), File(temporary.root, "wallpaper.img").readBytes())
        // Simulate a stale/missing materialized image; JSON remains authoritative.
        File(temporary.root, "wallpaper.img").writeBytes(byteArrayOf(9))
        store.save(data.copy(theme = 2))
        assertArrayEquals(byteArrayOf(3, 4), BackupCodec.decode(store.dump(data)).wallpaper)
        assertThrows(Exception::class.java) { store.saveWallpaper(data, byteArrayOf(), false) }
        assertEquals(2, store.load().theme)
        assertArrayEquals(byteArrayOf(3, 4), File(temporary.root, "wallpaper.img").readBytes())
    }
}
