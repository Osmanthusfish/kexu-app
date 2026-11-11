package com.schedule.app.data

import com.schedule.app.model.Course

data class CourseSlot(val course: Course, val column: Int, val columns: Int)

/** Partition connected overlaps, including courses with different starting periods. */
fun layoutCourses(courses: List<Course>, periodCount: Int): List<CourseSlot> {
    val sorted = courses.filter { it.startPeriod in 1..periodCount && it.endPeriod in it.startPeriod..periodCount }
        .sortedWith(compareBy({ it.startPeriod }, { it.endPeriod }, { it.id }))
    val result = mutableListOf<CourseSlot>()
    val group = mutableListOf<Course>()
    var groupEnd = 0
    fun flush() {
        val ends = mutableListOf<Int>()
        val placed = group.map { course ->
            var column = ends.indexOfFirst { it < course.startPeriod }
            if (column < 0) { column = ends.size; ends.add(course.endPeriod) } else ends[column] = course.endPeriod
            course to column
        }
        result.addAll(placed.map { (course, column) -> CourseSlot(course, column, ends.size) })
        group.clear()
    }
    sorted.forEach { course ->
        if (group.isNotEmpty() && course.startPeriod > groupEnd) flush()
        if (group.isEmpty()) groupEnd = 0
        group.add(course)
        groupEnd = maxOf(groupEnd, course.endPeriod)
    }
    flush()
    return result
}
