package com.schedule.app

import com.schedule.app.data.*
import com.schedule.app.model.Course
import com.schedule.app.ui.*
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime

class DailyExperienceTest {
    private fun course(id: String, start: Int, end: Int) = Course(id, id, "", "王老师", "A101", 1, start, end, "1-16")

    @Test fun pastCoursesCannotDisplaceTheNextClass() {
        val result = remainingCourses(listOf(course("past", 1, 2), course("next", 11, 12), course("afternoon", 5, 6)), emptyList(), LocalTime.of(20, 40))
        assertEquals(listOf("next"), result.map { it.id })
    }
    @Test fun concurrentActiveClassesAreBothRetained() {
        val result = remainingCourses(listOf(course("next", 5, 6), course("long", 1, 4), course("short", 3, 3)), emptyList(), LocalTime.of(10, 15))
        assertEquals(setOf("long", "short"), result.take(2).map { it.id }.toSet())
        assertEquals("next", result.last().id)
    }
    private fun assertLayout(vararg courses: Course) {
        val slots = layoutCourses(courses.toList(), 12)
        assertEquals(courses.size, slots.size)
        slots.forEach { assertTrue(it.column in 0 until it.columns) }
        for (a in slots) for (b in slots) {
            if (a.course.id == b.course.id) continue
            val overlap = maxOf(a.course.startPeriod, b.course.startPeriod) <= minOf(a.course.endPeriod, b.course.endPeriod)
            if (overlap) assertNotEquals("${a.course.id} covers ${b.course.id}", a.column, b.column)
        }
        assertEquals(courses.toSet(), slots.map { it.course }.toSet())
    }
    @Test fun sameStartDifferentEndKeepsBothDurations() = assertLayout(course("short", 1, 2), course("long", 1, 5))
    @Test fun nestedAndCrossingClassesStaySeparate() = assertLayout(course("long", 1, 7), course("inner", 2, 3), course("cross", 3, 5), course("tail", 6, 8))
    @Test fun adjoiningClassesCanReuseAColumn() {
        val result = layoutCourses(listOf(course("long", 1, 6), course("one", 1, 2), course("two", 3, 4), course("three", 5, 6)), 12)
        assertTrue(result.all { it.columns == 2 })
        assertEquals(1, result.filter { it.course.id != "long" }.map { it.column }.distinct().size)
    }
    @Test fun titleMeasurementUsesTheSameSizeAsRendering() {
        fun width(s: String, fs: Float, weight: FontWeight): Float = s.fold(0f) { sum, char -> sum + if (char.code >= 0x2E80) 1f else .62f } * fs * 2f
        val name = "示例课程戊戊戊戊戊实验"
        val room = "示例实验楼-XX3115"
        val teacher = "王老师"
        val fit = fitCourseFontMeasured(78f, 170f, name, room, teacher, densityPx = 2f, measureWidth = ::width)
        assertTrue("Unexpected tiny type ${fit.fs}", fit.fs >= 6f)
        assertEquals(name, fit.nameLines.joinToString(""))
        assertEquals(room, fit.roomLines.joinToString(""))
        assertEquals(teacher, fit.teacherLines.joinToString(""))
        assertTrue(fit.showTeacher)
        assertTrue(fit.measuredHeightPx <= 170f)
        fit.nameLines.forEach { assertTrue(width(it, courseNameFontSize(fit.fs), FontWeight.SemiBold) <= 78f) }
    }
    @Test fun actualHeightCanRejectAMathematicallyFittingCandidate() {
        val fit = fitCourseFontMeasured(90f, 100f, "课程名", "实验楼-A101", "王老师",
            measureWidth = { s, fs, _ -> s.length * fs },
            measureHeight = { s, fs, _ -> (s.count { it == '\n' } + 1) * (fs * 1.26f + 5f) })
        assertTrue(fit.measuredHeightPx <= 100f)
        assertTrue(fit.showTeacher)
        assertEquals("王老师", fit.teacherLines.joinToString(""))
    }
    @Test fun tallNarrowOverlapDoesNotStretchIntoAnOversizedSingleCharacterColumn() {
        val name = "示例课程戊戊戊戊戊实验"
        val fit = fitCourseFontMeasured(36f, 430f, name, "示例实验楼-XX3115", "王老师", densityPx = 2f,
            measureWidth = { s, fs, _ -> s.length * fs * 2f })
        assertEquals(name, fit.nameLines.joinToString(""))
        assertTrue(fit.nameLines.any { it.length >= 2 })
        assertTrue(fit.teacherLines.joinToString("") == "王老师")
        assertTrue(fit.measuredHeightPx <= 430f)
    }
    @Test fun balancingDoesNotLeaveAnOpeningBracketAtTheEndOfTheCourseNameLine() {
        val name = "高等数学（二）"
        val fit = fitCourseFontMeasured(78f, 170f, name, "示例楼四栋-14301", "王老师", densityPx = 2f,
            measureWidth = { s, fs, _ -> s.sumOf { if (it.code >= 0x2E80) 1.0 else .62 }.toFloat() * fs * 2f })
        assertEquals(name, fit.nameLines.joinToString(""))
        assertTrue(fit.nameLines.none { it.endsWith("（") })
        assertTrue(fit.measuredHeightPx <= 170f)
    }
}
