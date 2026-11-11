package com.schedule.app

import com.schedule.app.data.*
import com.schedule.app.model.*
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class ReminderRulesTest {
    private val course = Course("c", "数学", "", "", "", 1, 1, 2, "1-4单")
    private val sem = Semester("s", "秋", "2026-09-07", listOf(course), periodStarts = listOf("09:00"))
    private val reminder = Reminder("r", "上课", 1, 8, 0, 10, "", true, 1, "s", "c")
    private val data = BackupCodec.emptyData().copy(semesters = listOf(sem), reminders = listOf(reminder))
    private fun next(now: String, r: Reminder = reminder, d: AppData = data): String =
        java.time.Instant.ofEpochMilli(ReminderRules.nextMillis(r, d, ZonedDateTime.parse(now))).atZone(java.time.ZoneId.of("Asia/Shanghai")).toString()

    @Test fun semesterIsolationAndLegacyTimePreserved() {
        val legacy = reminder.copy(id = "legacy", linkedSemesterId = null, linkedCourseId = null)
        val other = reminder.copy(id = "other", linkedSemesterId = "other")
        val d = data.copy(semesters = listOf(sem, sem.copy(id = "other", periodStarts = listOf("10:00"))), reminders = listOf(reminder, legacy, other))
        val result = ReminderRules.synchronize(d).reminders
        assertEquals(listOf(9, 8, 10), result.map { it.hour })
    }
    @Test fun courseWeeksAndSemesterStartRespected() {
        assertEquals("2026-09-07T08:50+08:00[Asia/Shanghai]", next("2026-08-01T12:00+08:00[Asia/Shanghai]"))
        assertEquals("2026-09-21T08:50+08:00[Asia/Shanghai]", next("2026-09-07T09:01+08:00[Asia/Shanghai]"))
        assertEquals(-1L, ReminderRules.nextMillis(reminder, data, ZonedDateTime.parse("2026-09-21T09:01+08:00[Asia/Shanghai]")))
    }
    @Test fun leadCanCrossMidnightAndWeekBoundary() {
        val d = data.copy(semesters = listOf(sem.copy(periodStarts = listOf("00:05"))))
        assertEquals("2026-09-06T23:55+08:00[Asia/Shanghai]", next("2026-09-06T23:50+08:00[Asia/Shanghai]", d = d))
    }
    @Test fun deletedCourseDisablesOnlyLinkedReminder() {
        val d = data.copy(semesters = emptyList())
        assertFalse(ReminderRules.synchronize(d).reminders.single().enabled)
        assertEquals(-1L, ReminderRules.nextMillis(reminder, d))
        val old = reminder.copy(linkedSemesterId = null, linkedCourseId = null)
        assertTrue(ReminderRules.synchronize(d.copy(reminders = listOf(old))).reminders.single().enabled)
    }
    @Test fun blankWeeksRepeatAndLegacyDoesNotFollowTerm() {
        val old = reminder.copy(linkedSemesterId = null, linkedCourseId = null)
        assertEquals("2026-08-03T07:50+08:00[Asia/Shanghai]", next("2026-08-01T12:00+08:00[Asia/Shanghai]", old))
        val d = data.copy(semesters = listOf(sem.copy(courses = listOf(course.copy(weeks = "")))))
        assertEquals("2027-01-04T08:50+08:00[Asia/Shanghai]", next("2027-01-01T12:00+08:00[Asia/Shanghai]", d = d))
    }
    @Test fun backupPreservesExplicitLinks() { assertEquals(data, BackupCodec.decode(BackupCodec.encode(BackupPayload(data, null, null))).data) }
    @Test fun weekExpressionsDoNotOverflow() {
        assertFalse(Wk.weeksContains("99999999999999999999", 1))
        assertTrue(Wk.weeksContains("1～4，6～8", 7))
        assertTrue(Wk.weeksContains("单", 3))
        assertFalse(Wk.weeksContains("双", -1))
    }
    @Test fun periodSettingsRejectInvalidOrReversedTimes() {
        assertNotNull(PeriodTimes.validate(listOf("08:00", "25:00")))
        assertNotNull(PeriodTimes.validate(listOf("08:00", "07:00")))
        assertNull(PeriodTimes.validate(listOf("8:00", "08:55")))
        // Do not silently change previously displayed fallback times during an upgrade.
        assertEquals(java.time.LocalTime.of(18, 50), PeriodTimes.default(14))
    }
}
