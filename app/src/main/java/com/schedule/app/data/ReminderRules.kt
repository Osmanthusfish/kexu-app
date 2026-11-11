package com.schedule.app.data

import com.schedule.app.model.AppData
import com.schedule.app.model.Reminder
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Pure rules: legacy reminders keep their weekly times; explicit links follow only their course. */
object ReminderRules {
    fun noteAtTrigger(r: Reminder, data: AppData, fireMillis: Long, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): String {
        val sem = data.semesters.find { it.id == r.linkedSemesterId } ?: return r.note
        val course = sem.courses.find { it.id == r.linkedCourseId } ?: return r.note
        val time = java.time.Instant.ofEpochMilli(fireMillis).atZone(zone).plusMinutes(r.leadMinutes.toLong())
        val occurrence = OccurrenceRules.onDate(data, sem, time.toLocalDate()).find {
            it.base.id == course.id && PeriodTimes.start(sem.periodStarts.ifEmpty { data.periodStarts }, it.course.startPeriod) == time.toLocalTime().withSecond(0).withNano(0)
        } ?: return r.note
        return listOf("教室：${occurrence.course.room.ifBlank { "未填写" }}", "教师：${occurrence.course.teacher.ifBlank { "未填写" }}",
            r.note.takeUnless { it == course.room }.orEmpty()).filter { it.isNotBlank() }.joinToString(" · ")
    }
    fun synchronize(data: AppData): AppData = data.copy(reminders = data.reminders.map { r ->
        if (r.linkedSemesterId == null || r.linkedCourseId == null) r else {
            val sem = data.semesters.find { it.id == r.linkedSemesterId }
            val course = sem?.courses?.find { it.id == r.linkedCourseId }
            if (course == null || course.weekday !in 1..7 || course.startPeriod !in 1..16) r.copy(enabled = false)
            else {
                val time = PeriodTimes.start(sem.periodStarts.ifEmpty { data.periodStarts }, course.startPeriod)
                r.copy(weekday = course.weekday, hour = time.hour, minute = time.minute, linkedPeriod = course.startPeriod)
            }
        }
    })

    fun nextMillis(r: Reminder, data: AppData, now: ZonedDateTime = ZonedDateTime.now()): Long {
        if (!r.enabled) return -1L
        val linked = r.linkedSemesterId != null && r.linkedCourseId != null
        val sem = data.semesters.find { it.id == r.linkedSemesterId }
        val course = sem?.courses?.find { it.id == r.linkedCourseId }
        if (linked && (course == null || course.weekday !in 1..7 || course.startPeriod !in 1..16)) return -1L
        if (linked) {
            val semester = sem!!
            val base = course!!
            val starts = semester.periodStarts.ifEmpty { data.periodStarts }
            val first = Wk.parseDateOrNull(semester.startDate) ?: return -1L
            val earliest = now.plusMinutes(r.leadMinutes.toLong()).toLocalDate()
            val dates = Wk.weeksIn(base.weeks, 1..520).asSequence()
                .map { Wk.weekDate(first, it, base.weekday) }.toSet() +
                data.courseChanges.filter { it.semesterId == semester.id && it.courseId == base.id && it.mode != "cancel" }.map { java.time.LocalDate.parse(it.date) }
            return dates.asSequence().filter { !it.isBefore(earliest) }.sorted().flatMap { date ->
                OccurrenceRules.onDate(data, semester, date).asSequence().filter { it.base.id == base.id }.map {
                    date.atTime(PeriodTimes.start(starts, it.course.startPeriod)).atZone(now.zone).minusMinutes(r.leadMinutes.toLong())
                }
            }.filter { it.isAfter(now) }.minOrNull()?.toInstant()?.toEpochMilli() ?: -1L
        }
        val start = if (linked) Wk.parseDateOrNull(sem!!.startDate) ?: return -1L else null
        val weekday = if (linked) course!!.weekday else r.weekday
        val time = if (linked) PeriodTimes.start(sem!!.periodStarts.ifEmpty { data.periodStarts }, course!!.startPeriod)
                   else LocalTime.of(r.hour, r.minute)
        // Search class dates after the lead offset (including next week's class whose reminder is this week).
        var date = now.plusMinutes(r.leadMinutes.toLong()).toLocalDate()
        if (start != null && date.isBefore(start)) date = start
        date = date.plusDays(((weekday - date.dayOfWeek.value + 7) % 7).toLong())
        repeat(if (linked) 522 else 2) {
            val week = if (start != null) Math.floorDiv(ChronoUnit.DAYS.between(start, date), 7).toInt() + 1 else 1
            val fire = date.atTime(time).atZone(now.zone).minusMinutes(r.leadMinutes.toLong())
            if ((!linked || (week >= 1 && Wk.weeksContains(course!!.weeks, week))) && fire.isAfter(now)) return fire.toInstant().toEpochMilli()
            date = date.plusWeeks(1)
        }
        return -1L
    }
}
