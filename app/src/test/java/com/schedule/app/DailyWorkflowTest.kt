package com.schedule.app

import com.schedule.app.data.*
import com.schedule.app.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.json.JSONObject
import java.io.File
import java.time.*

class DailyWorkflowTest {
    @get:Rule val temporary = TemporaryFolder()
    private val course = Course("math", "高等数学", "", "王老师", "A101", 1, 1, 2, "1-16单")
    private val sem = Semester("autumn", "秋季", "2026-09-07", listOf(course))
    private val original = BackupCodec.emptyData().copy(semesters = listOf(sem))
    private val monday = LocalDate.parse("2026-11-02") // week 9, odd
    private fun change(mode: String = "move", date: String = "2026-11-03") = CourseChange("change", sem.id, course.id, monday.toString(), date, mode, 3, 4, "实验楼-B201")
    private fun reminder() = Reminder("alarm", course.name, 1, 8, 0, 10, "", true, 1, sem.id, course.id)
    private fun now(s: String) = LocalDateTime.parse(s).atZone(ZoneId.of("Asia/Shanghai"))

    @Test fun moveChangesOnlyOneDateAndKeepsTheOriginalCourse() {
        val data = OccurrenceRules.put(original, change())
        assertEquals(original.semesters, data.semesters)
        assertTrue(OccurrenceRules.onDate(data, sem, monday).isEmpty())
        val moved = OccurrenceRules.onDate(data, sem, monday.plusDays(1)).single()
        assertEquals("实验楼-B201", moved.course.room)
        assertEquals(3, moved.course.startPeriod)
        assertEquals(4, moved.course.endPeriod)
        assertEquals(monday, moved.originalDate)
        assertEquals(course, OccurrenceRules.onDate(data, sem, monday.plusWeeks(2)).single().course)
        assertTrue(OccurrenceRules.onDate(data, sem, monday.plusWeeks(1)).isEmpty())
    }
    @Test fun cancelledClassDoesNotFireAndDeletingChangeRestoresIt() {
        val data = OccurrenceRules.put(original, change("cancel"))
        assertTrue(OccurrenceRules.onDate(data, sem, monday).isEmpty())
        val fire = Instant.ofEpochMilli(ReminderRules.nextMillis(reminder(), data, now("2026-11-01T12:00"))).atZone(now("2026-11-01T12:00").zone)
        assertEquals(monday.plusWeeks(2), fire.toLocalDate())
        assertEquals(course, OccurrenceRules.onDate(data.copy(courseChanges = emptyList()), sem, monday).single().course)
    }
    @Test fun movedReminderUsesNewDayAndPeriodAcrossMidnight() {
        val earlySem = sem.copy(periodStarts = listOf("00:05", "01:00", "02:00", "03:00"))
        val data = OccurrenceRules.put(original.copy(semesters = listOf(earlySem)), change().copy(startPeriod = 1, endPeriod = 2))
        assertEquals(now("2026-11-02T23:55").toInstant().toEpochMilli(), ReminderRules.nextMillis(reminder(), data, now("2026-11-01T12:00")))
    }
    @Test fun makeupIsAdditionalEvenOnAnOtherwiseEmptyWeek() {
        val data = OccurrenceRules.put(original, change("extra", "2026-11-09"))
        assertEquals(1, OccurrenceRules.onDate(data, sem, monday).size)
        assertEquals(1, OccurrenceRules.onDate(data, sem, monday.plusWeeks(1)).size)
    }
    @Test fun changingAnExceptionDoesNotLeaveOldTargetsBehind() {
        val data = OccurrenceRules.put(OccurrenceRules.put(original, change()), change().copy(id = "replacement", date = "2026-11-04"))
        assertEquals(1, data.courseChanges.size)
        assertTrue(OccurrenceRules.onDate(data, sem, monday.plusDays(1)).isEmpty())
        assertEquals(1, OccurrenceRules.onDate(data, sem, monday.plusDays(2)).size)
    }
    @Test fun deletedCoursesAndOtherSemestersNeverLeakIntoTimetable() {
        val other = sem.copy(id = "spring")
        val data = OccurrenceRules.put(original, change())
        assertEquals(course, OccurrenceRules.onDate(data, other, monday).single().course)
        assertTrue(OccurrenceRules.onDate(data, sem.copy(courses = emptyList()), monday.plusDays(1)).isEmpty())
    }
    @Test fun repeatedBatchPreservesIndividualExceptionsAndDoesNotDuplicate() {
        val once = BatchReminderRules.apply(original, sem.id, setOf(course.id), 10, false)
        val custom = once.copy(reminders = once.reminders.map { it.copy(leadMinutes = 25, enabled = false) })
        val twice = BatchReminderRules.apply(custom, sem.id, setOf(course.id), 5, false)
        assertEquals(custom.reminders, twice.reminders)
        val replaced = BatchReminderRules.apply(twice, sem.id, setOf(course.id), 5, true)
        assertEquals(1, replaced.reminders.size)
        assertEquals(5, replaced.reminders.single().leadMinutes)
        assertTrue(replaced.reminders.single().enabled)
    }
    @Test fun taskCompletionAndMissingDeadlineDisableAlerts() {
        val task = Todo("todo", "作业", null, "2026-11-03", false, sem.id, course.id, "00:05", 10)
        assertEquals(now("2026-11-02T23:55").toInstant().toEpochMilli(), TodoRules.nextMillis(task, now("2026-11-02T12:00")))
        assertEquals(-1L, TodoRules.nextMillis(task.copy(done = true), now("2026-11-02T12:00")))
        assertEquals(-1L, TodoRules.nextMillis(task.copy(dueDate = null), now("2026-11-02T12:00")))
        assertEquals(-1L, TodoRules.nextMillis(task, now("2026-11-04T12:00")))
    }
    @Test fun newFieldsRoundTripAndLegacyFieldsStillLoad() {
        val data = OccurrenceRules.put(original, change()).copy(todos = listOf(Todo("t", "实验报告", null, "2026-11-03", false, sem.id, course.id, "18:30", 60)))
        val encoded = BackupCodec.encode(BackupPayload(data, null, null))
        assertEquals(4, JSONObject(encoded).getInt("schemaVersion"))
        assertEquals(data, BackupCodec.decode(encoded).data)
        val legacy = JSONObject(BackupCodec.encode(BackupPayload(original, null, null))).apply { put("schemaVersion", 1); remove("courseChanges") }
        assertEquals(original, BackupCodec.decode(legacy.toString()).data)
    }
    @Test fun upgradeKeepsExactOriginalBytesAcrossRepeatedSavesAndImports() {
        val legacy = JSONObject(BackupCodec.encode(BackupPayload(original, null, null))).apply { put("schemaVersion", 1); remove("courseChanges") }.toString(2)
        File(temporary.root, "data.json").writeText(legacy)
        val store = Store(temporary.root)
        val data = OccurrenceRules.put(store.load(), change())
        store.save(data)
        store.save(data.copy(theme = 2))
        store.importBackup(BackupCodec.encode(BackupPayload(original, null, null)), data)
        assertEquals(legacy, File(temporary.root, "recovery/before-v143-upgrade.json").readText())
    }
    @Test fun invalidChangesAndDeadlinesAreRejectedBeforeWriting() {
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.validate(OccurrenceRules.put(original, change().copy(endPeriod = 0))) }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.validate(original.copy(todos = listOf(Todo("t", "x", null, null, false, remindMinutesBefore = 1)))) }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.validate(original.copy(courseChanges = listOf(change(), change().copy(id = "duplicate")))) }
    }
    @Test fun invalidNegativeReminderInBackupIsNotSilentlyTurnedOff() {
        val data = original.copy(todos = listOf(Todo("t", "作业", null, "2026-11-03", false, remindMinutesBefore = 10)))
        val malformed = JSONObject(BackupCodec.encode(BackupPayload(data, null, null)))
        malformed.getJSONArray("todos").getJSONObject(0).put("remindMinutesBefore", -2)
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(malformed.toString()) }
    }
    @Test fun widgetRefreshesAtActualStartEndAndMidnight() {
        val moved = OccurrenceRules.put(original, change())
        val tuesday = now("2026-11-03T08:00")
        val starts = PeriodTimes.start(emptyList(), 3)
        assertEquals(tuesday.toLocalDate().atTime(starts).atZone(tuesday.zone).toInstant().toEpochMilli(), WidgetRules.nextRefresh(moved, sem, tuesday))
        val during = tuesday.toLocalDate().atTime(starts.plusMinutes(1)).atZone(tuesday.zone)
        assertEquals(tuesday.toLocalDate().atTime(PeriodTimes.start(emptyList(), 4).plusMinutes(45)).atZone(tuesday.zone).toInstant().toEpochMilli(), WidgetRules.nextRefresh(moved, sem, during))
        assertEquals(now("2026-11-04T00:00").toInstant().toEpochMilli(), WidgetRules.nextRefresh(moved, sem, now("2026-11-03T23:00")))
    }
    @Test fun widgetFindsNextDayAndRescheduledClassesBeforeSemesterStart() {
        val makeup = change("extra", "2026-09-01")
        val data = OccurrenceRules.put(original, makeup)
        assertEquals(LocalDate.parse("2026-09-01"), OccurrenceRules.upcoming(data, sem, now("2026-08-31T12:00"), 1).single().date)
        assertEquals(monday.plusWeeks(2), OccurrenceRules.upcoming(original, sem, now("2026-11-02T23:00"), 1).single().date)
    }
    @Test fun movedAlarmContentUsesTheActualClassroomAndKeepsCustomNotes() {
        val data = OccurrenceRules.put(original, change())
        val alarm = reminder().copy(note = "带实验报告")
        val fire = ReminderRules.nextMillis(alarm, data, now("2026-11-01T12:00"))
        val note = ReminderRules.noteAtTrigger(alarm, data, fire, now("2026-11-01T12:00").zone)
        assertTrue(note.contains("实验楼-B201"))
        assertTrue(note.contains("王老师"))
        assertTrue(note.contains("带实验报告"))
    }
}
