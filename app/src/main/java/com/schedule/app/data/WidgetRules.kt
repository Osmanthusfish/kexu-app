package com.schedule.app.data

import com.schedule.app.model.*
import java.time.ZonedDateTime

object WidgetRules {
    fun nextRefresh(data: AppData, sem: Semester?, now: ZonedDateTime): Long {
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        if (sem == null) return nextMidnight.toInstant().toEpochMilli()
        val starts = sem.periodStarts.ifEmpty { data.periodStarts }
        val boundaries = OccurrenceRules.onDate(data, sem, now.toLocalDate()).flatMap { o -> listOf(
            OccurrenceRules.startsAt(o, starts, now.zone),
            OccurrenceRules.endsAt(o, starts, now.zone)
        ) }.filter { it.isAfter(now) }
        return (boundaries + nextMidnight).minOrNull()!!.toInstant().toEpochMilli()
    }
    fun agenda(data: AppData, sem: Semester, now: ZonedDateTime): List<CourseOccurrence> {
        val starts = sem.periodStarts.ifEmpty { data.periodStarts }
        return OccurrenceRules.onDate(data, sem, now.toLocalDate()).sortedWith(compareBy<CourseOccurrence> {
            !OccurrenceRules.endsAt(it, starts, now.zone).isAfter(now)
        }.thenBy { it.course.startPeriod }.thenBy { it.key })
    }
}
