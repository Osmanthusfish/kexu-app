package com.schedule.app.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 日期 / 周次 / 课程周数 计算工具。 */
object Wk {

    fun today(): LocalDate = LocalDate.now()

    /** 用户输入必须是存在的日期；不要把格式正确但不存在的日期当成有效输入。 */
    fun parseDateOrNull(s: String): LocalDate? {
        val value = s.trim()
        if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) return null
        return runCatching { LocalDate.parse(value) }.getOrNull()
    }

    /** 旧数据展示的兜底；编辑/导入校验使用 parseDateOrNull。 */
    fun parseISO(s: String): LocalDate = parseDateOrNull(s) ?: today()

    /** 周几，周一=1 ... 周日=7。 */
    fun dayOfWeek(d: LocalDate): Int = d.dayOfWeek.value

    /** 第 week 周（week>=1）的第 day（1..7）天的日期。 */
    fun weekDate(start: LocalDate, week: Int, day: Int): LocalDate =
        start.plusDays((week.toLong() - 1L) * 7L + (day - 1))

    /** 今天是第几周（从 start 那周算起）。开学前为 0 或负数，可无限延伸。 */
    fun currentWeek(start: LocalDate): Int {
        val days = ChronoUnit.DAYS.between(start, today()).toInt()
        return Math.floorDiv(days, 7) + 1
    }

    /** 今天是否落在第 week 周（当周范围含今天）。 */
    fun isWeekOf(start: LocalDate, week: Int): Boolean =
        today() in weekDate(start, week, 1)..weekDate(start, week, 7)

    /** 判断课程周数描述（如 "1-4,6-9" / "1-16单" / "1-16双"）是否包含第 w 周。空串表示每周都有。 */
    private val weekRange = Regex("(\\d+)(?:[-~](\\d+))?")
    private data class WeekPattern(val blank: Boolean, val odd: Boolean, val even: Boolean,
                                   val numbered: Boolean, val ranges: List<IntRange>) {
        fun contains(w: Int): Boolean {
            if (blank) return true
            if (w < 1 || (odd && w % 2 == 0) || (even && w % 2 != 0)) return false
            return if (!numbered) odd || even else ranges.any { w in it }
        }
    }
    // Immutable patterns are shared by UI, alarms and widgets. Bound user-supplied keys in memory.
    private val weekPatterns = object : LinkedHashMap<String, WeekPattern>(128, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, WeekPattern>?) = size > 128
    }
    @Synchronized private fun pattern(weeks: String): WeekPattern = weekPatterns.getOrPut(weeks) {
        WeekPattern(weeks.isBlank(), weeks.contains("单"), weeks.contains("双"), weeks.any { it.isDigit() },
            weeks.replace('，', ',').replace('～', '~').split(",").mapNotNull { piece ->
                val match = weekRange.find(piece.trim()) ?: return@mapNotNull null
                val first = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                val last = if (match.groupValues[2].isEmpty()) first else match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
                first..last
            })
    }
    fun weeksContains(weeks: String, w: Int): Boolean = pattern(weeks).contains(w)

    /** Parse once for a multi-week search instead of reparsing up to 520 times per course. */
    fun weeksIn(weeks: String, range: IntRange): List<Int> = pattern(weeks).let { rule -> range.filter(rule::contains) }

    /** 两个日期相差的天数。 */
    fun daysBetween(a: LocalDate, b: LocalDate): Long = ChronoUnit.DAYS.between(a, b)

    /** 把任意日期归到它所在周的周一。用户填的"开学日"如果不是周一，按这个对齐。 */
    fun mondayOf(d: LocalDate): LocalDate = d.minusDays((d.dayOfWeek.value - 1).toLong())
}
