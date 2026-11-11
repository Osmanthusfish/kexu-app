package com.schedule.app

import android.content.Context
import android.content.res.Configuration
import com.schedule.app.data.*
import com.schedule.app.model.*
import com.schedule.app.ui.WidgetPreviewCache
import com.schedule.app.ui.WidgetPreviewKey
import kotlinx.coroutines.runBlocking
import java.time.*

/** Fixed synthetic data only: the runner is restricted to the isolated emulator package. */
internal object PerformanceChecks {
    fun run(context: Context, report: (String) -> Unit) = runBlocking {
        val courses = (0 until 40).map { Course("perf-$it", "测试课程 $it", "", "测试教师", "A101", it % 7 + 1, it % 10 + 1, it % 10 + 2, if (it % 2 == 0) "1-16单" else "1-4,6-18") }
        val sem = Semester("perf", "性能测试", "2026-09-07", courses)
        val data = BackupCodec.emptyData().copy(semesters = listOf(sem))
        val now = LocalDateTime.parse("2026-11-10T13:00").atZone(ZoneId.of("Asia/Shanghai"))
        fun previousContains(weeks: String, w: Int): Boolean {
            if (weeks.isBlank()) return true
            if (w < 1) return false
            if (weeks.contains("单") && w % 2 == 0) return false
            if (weeks.contains("双") && w % 2 != 0) return false
            if (!weeks.any { it.isDigit() }) return weeks.contains("单") || weeks.contains("双")
            return weeks.replace('，', ',').replace('～', '~').split(",").any { piece ->
                val m = Regex("(\\d+)(?:[-~](\\d+))?").find(piece.trim()) ?: return@any false
                val a = m.groupValues[1].toIntOrNull() ?: return@any false
                val b = if (m.groupValues[2].isEmpty()) a else m.groupValues[2].toIntOrNull() ?: return@any false
                w in a..b
            }
        }
        fun oldUpcoming(): List<CourseOccurrence> {
            val earliest = now.toLocalDate().minusDays(1)
            val dates = courses.flatMap { c -> (maxOf(1, OccurrenceRules.week(sem, earliest))..520)
                .filter { previousContains(c.weeks, it) }.map { Wk.weekDate(LocalDate.parse(sem.startDate), it, c.weekday) } }.toSet()
            val result = mutableListOf<CourseOccurrence>()
            for (date in dates.filter { !it.isBefore(earliest) }.sorted()) {
                result += OccurrenceRules.onDate(data, sem, date).filter { OccurrenceRules.endsAt(it, emptyList(), now.zone).isAfter(now) }
                if (result.size >= 1) break
            }
            return result.take(1)
        }
        val oldMs = mutableListOf<Double>(); val newMs = mutableListOf<Double>()
        repeat(3) {
            val t = System.nanoTime(); val old = oldUpcoming(); oldMs += (System.nanoTime() - t) / 1e6
            val n = System.nanoTime(); val next = OccurrenceRules.upcoming(data, sem, now, 1); newMs += (System.nanoTime() - n) / 1e6
            check(old == next)
        }
        report("Upcoming 40 synthetic courses, same results: oldMs=$oldMs newMs=$newMs")
        val cache = WidgetPreviewCache()
        val key = WidgetPreviewKey(data, false, 332f, 303f, 0, LocalDateTime.now().withSecond(0).withNano(0), Configuration(context.resources.configuration))
        val first = cache.render(context, key)
        check(cache.render(context, key) === first)
        val variants = listOf(key.copy(wallpaperRevision = 1), key.copy(minute = key.minute.plusDays(1)), key.copy(width = 180f), key.copy(weekly = true))
        for (other in variants) { check(cache.get(other) == null); check(cache.render(context, other) !== first) }
        check(cache.get(key) == null) // bounded cache evicts, never retains every slider position
    }
}
