package com.schedule.app.data

import com.schedule.app.model.*
import java.time.LocalDate
import java.time.ZonedDateTime

data class WidgetOverlapGroup(val start: Int, val end: Int, val slots: List<CourseSlot>)
data class WidgetWeek(val monday: LocalDate, val week: Int, val days: List<Int>, val periods: Int, val occurrences: List<CourseOccurrence>)

object WidgetSchedule {
    fun week(data: AppData, sem: Semester, date: LocalDate): WidgetWeek {
        val monday = Wk.mondayOf(date)
        val week = OccurrenceRules.week(sem, monday)
        val all = OccurrenceRules.inWeek(data, sem, week)
        // Weekend courses always remain visible, even when the user prefers five empty-day columns.
        val days = (1..7).filter { it <= 5 || data.visual.widgetWeekends || all.any { o -> o.course.weekday == it } }
        val periods = (all.maxOfOrNull { it.course.endPeriod } ?: sem.periodsPerDay.takeIf { it > 0 } ?: data.periodsPerDay).coerceIn(4,16)
        return WidgetWeek(monday, week, days, periods, all)
    }
    fun groups(courses: List<Course>, periods: Int): List<WidgetOverlapGroup> {
        val slots = layoutCourses(courses, periods)
        val groups = mutableListOf<WidgetOverlapGroup>()
        var current = mutableListOf<CourseSlot>()
        var end = 0
        for (slot in slots) {
            if (current.isNotEmpty() && slot.course.startPeriod > end) {
                groups += WidgetOverlapGroup(current.minOf { it.course.startPeriod }, end, current.toList())
                current = mutableListOf(); end = 0
            }
            current += slot; end = maxOf(end,slot.course.endPeriod)
        }
        if (current.isNotEmpty()) groups += WidgetOverlapGroup(current.minOf { it.course.startPeriod },end,current.toList())
        return groups
    }
    fun today(data: AppData, sem: Semester, now: ZonedDateTime): List<CourseOccurrence> = OccurrenceRules.onDate(data,sem,now.toLocalDate())
    fun pageCount(total: Int, capacity: Int) = maxOf(1,(total+capacity.coerceAtLeast(1)-1)/capacity.coerceAtLeast(1))
    fun boundedPage(page: Int,total: Int,capacity: Int) = page.coerceIn(0,pageCount(total,capacity)-1)
    fun dailyCapacity(height: Float, fontScale: Float, tasks: Boolean): Int {
        val usable = height-30-44-24-(if(tasks && height>=230) 32 else 0)
        return (usable/(64*fontScale.coerceIn(.8f,1.3f))).toInt().coerceIn(1,5)
    }
}
