package com.schedule.app.data

import com.schedule.app.model.*
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

data class CourseOccurrence(val base: Course, val course: Course, val date: LocalDate, val originalDate: LocalDate, val changeId: String? = null) {
    val key: String get() = changeId?.let { "change:$it" } ?: "course:${base.id}:$date"
}

/** Shared by the grid, home, alarms and both desktop widgets. */
object OccurrenceRules {
    fun startsAt(o: CourseOccurrence, starts: List<String>, zone: java.time.ZoneId): ZonedDateTime =
        o.date.atTime(PeriodTimes.start(starts, o.course.startPeriod)).atZone(zone)
    fun endsAt(o: CourseOccurrence, starts: List<String>, zone: java.time.ZoneId): ZonedDateTime =
        o.date.atTime(PeriodTimes.start(starts, o.course.endPeriod)).atZone(zone).plusMinutes(45)

    fun week(sem: Semester, date: LocalDate): Int = Math.floorDiv(ChronoUnit.DAYS.between(LocalDate.parse(sem.startDate), date), 7).toInt() + 1

    fun onDate(data: AppData, sem: Semester, date: LocalDate): List<CourseOccurrence> {
        val changes = data.courseChanges.filter { it.semesterId == sem.id }
        val week = week(sem, date)
        val regular = sem.courses.filter { c ->
            week >= 1 && c.weekday == date.dayOfWeek.value && c.startPeriod in 1..16 && c.endPeriod in c.startPeriod..16 &&
                Wk.weeksContains(c.weeks, week) && changes.none { it.courseId == c.id && it.originalDate == date.toString() && it.mode != "extra" }
        }.map { CourseOccurrence(it, it, date, date) }
        val altered = changes.filter { it.date == date.toString() && it.mode != "cancel" }.mapNotNull { change ->
            val base = sem.courses.find { it.id == change.courseId } ?: return@mapNotNull null
            CourseOccurrence(base, base.copy(weekday = date.dayOfWeek.value, startPeriod = change.startPeriod,
                endPeriod = change.endPeriod, room = change.room), date, LocalDate.parse(change.originalDate), change.id)
        }
        return (regular + altered).sortedWith(compareBy({ it.course.startPeriod }, { it.course.endPeriod }, { it.key }))
    }

    fun inWeek(data: AppData, sem: Semester, week: Int): List<CourseOccurrence> = (1..7).flatMap {
        onDate(data, sem, Wk.weekDate(LocalDate.parse(sem.startDate), week, it))
    }

    fun upcoming(data: AppData, sem: Semester, now: ZonedDateTime, limit: Int = 4): List<CourseOccurrence> {
        val starts = sem.periodStarts.ifEmpty { data.periodStarts }
        val result = mutableListOf<CourseOccurrence>()
        val earliest = now.toLocalDate().minusDays(1)
        val first = LocalDate.parse(sem.startDate)
        val dates = sem.courses.filter { it.weekday in 1..7 && it.startPeriod in 1..16 && it.endPeriod in it.startPeriod..16 }.flatMap { c ->
            Wk.weeksIn(c.weeks, maxOf(1, week(sem, earliest))..520).map { Wk.weekDate(first, it, c.weekday) }
        }.toSet() + data.courseChanges.filter { it.semesterId == sem.id && it.mode != "cancel" }.map { LocalDate.parse(it.date) }
        for (date in dates.filter { !it.isBefore(earliest) }.sorted()) {
            result += onDate(data, sem, date).filter {
                endsAt(it, starts, now.zone).isAfter(now)
            }
            if (result.size >= limit) break
        }
        return result.take(limit)
    }

    fun put(data: AppData, change: CourseChange): AppData = data.copy(courseChanges = data.courseChanges.filterNot {
        it.id == change.id || (change.mode != "extra" && it.mode != "extra" && it.semesterId == change.semesterId && it.courseId == change.courseId && it.originalDate == change.originalDate)
    } + change)
}
