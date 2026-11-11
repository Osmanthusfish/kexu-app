package com.schedule.app.data

import com.schedule.app.model.Course
import java.util.UUID

/** 通用课表表格解析：列名自动识别 + 上课时间正则拆解。 */
object Parser {

    data class ColMap(val name: Int, val code: Int, val time: Int, val room: Int, val teacher: Int)

    private val NAME_KW    = listOf("课程名称", "课程名", "课程", "科目", "名称")
    private val CODE_KW    = listOf("教学班号", "班号", "课程代码", "课程编码", "代码", "编号")
    private val TIME_KW    = listOf("上课时间", "授课时间", "时间")
    private val ROOM_KW    = listOf("上课地点", "教室", "地点")
    private val TEACHER_KW = listOf("上课教师", "授课教师", "任课教师", "教师", "老师")

    /** 找到表头行下标：所在行包含 上课时间/课程名称 等关键词。找不到返回 -1。 */
    fun findHeaderRow(grid: List<List<String>>): Int =
        grid.indexOfFirst { row -> row.any { c -> TIME_KW.any { c.contains(it) } } }

    /** 根据表头自动映射列下标。name/time 缺失时返回 null，需要用户手动映射。 */
    fun autoMap(header: List<String>): ColMap? {
        fun find(kw: List<String>) = header.indexOfFirst { c -> kw.any { c.contains(it) } }
        val name = find(NAME_KW)
        val time = find(TIME_KW)
        if (name == -1 || time == -1) return null
        return ColMap(
            name = name,
            code = find(CODE_KW),
            time = time,
            room = find(ROOM_KW),
            teacher = find(TEACHER_KW)
        )
    }

    fun parse(grid: List<List<String>>, headerRow: Int, cm: ColMap, periodStarts: List<String> = emptyList()): List<Course> {
        require(headerRow in grid.indices && cm.name >= 0 && cm.time >= 0) { "请选择有效的表头、课程名称和上课时间列" }
        return grid.drop(headerRow + 1).flatMap { row ->
            val name = row.getOrNull(cm.name)?.trim().orEmpty()
            if (name.isBlank()) emptyList() else parseTimes(row.getOrNull(cm.time).orEmpty(), periodStarts).map { t ->
                Course(UUID.randomUUID().toString(), name, row.getOrNull(cm.code)?.trim().orEmpty(),
                    row.getOrNull(cm.teacher)?.trim().orEmpty(), row.getOrNull(cm.room)?.trim().orEmpty(),
                    t.weekday, t.startPeriod, t.endPeriod, t.weeks)
            }
        }
    }

    data class TimeParts(val weekday: Int, val startPeriod: Int, val endPeriod: Int, val weeks: String)
    private val dayPattern = Regex("(?:星期|周)([一二三四五六日天])")
    private val weekPattern = Regex("((?:\\d+(?:-\\d+)?)(?:,\\d+(?:-\\d+)?)*)周(?:[（(]?([单双])[）)]?)?")
    private val periodPattern = Regex("((?:\\d+(?:-\\d+)?)(?:,\\d+(?:-\\d+)?)*)节")
    private fun normalize(s: String) = s.replace('，', ',').replace('～', '-').replace('~', '-').replace(" ", "").replace("\r", "")

    /** One result per separate weekday/period interval; never fill gaps between classes. */
    fun parseTimes(raw: String, periodStarts: List<String> = emptyList()): List<TimeParts> {
        val parts = normalize(raw).split(Regex("[;；\\n]+"))
        return parts.filter { it.isNotBlank() }.flatMap { text ->
            val days = dayPattern.findAll(text).toList()
            val prefix = days.firstOrNull()?.let { text.substring(0, it.range.first) }.orEmpty()
            if (days.size <= 1) parseSingle(text, periodStarts) else {
                val suffixes = days.mapIndexed { i, day -> text.substring(day.range.last + 1, days.getOrNull(i + 1)?.range?.first ?: text.length) }
                val sharedSuffix = suffixes.last()
                days.flatMapIndexed { i, day ->
                    val before = text.substring(if (i == 0) 0 else days[i - 1].range.last + 1, day.range.first)
                    val weeks = weekPattern.findAll(before).lastOrNull()?.value ?: prefix
                    val suffix = suffixes[i].replace(weekPattern, "").trim(',', '、')
                    parseSingle(weeks + day.value + suffix.ifBlank { sharedSuffix }, periodStarts)
                }
            }
        }.ifEmpty { listOf(TimeParts(0, 0, 0, "")) }.distinct()
    }

    /** Kept for callers that need a single arrangement; imports use parseTimes. */
    fun parseTime(sRaw: String): TimeParts = parseTimes(sRaw).first()

    private fun parseSingle(text: String, config: List<String>): List<TimeParts> {
        val day = dayPattern.find(text)?.groupValues?.get(1)?.let { "一二三四五六日天".indexOf(it) + 1 }?.coerceAtMost(7) ?: 0
        val week = weekPattern.find(text)
        val parity = week?.groupValues?.get(2)?.ifEmpty { null } ?: when { text.contains("单") -> "单"; text.contains("双") -> "双"; else -> "" }
        val weeks = week?.groupValues?.get(1).orEmpty() + parity
        val intervals = periodPattern.findAll(text).flatMap { match ->
            match.groupValues[1].split(',').map { piece ->
                val bounds = piece.split('-')
                val start = bounds[0].toIntOrNull() ?: error("节次数字过大")
                val end = bounds.getOrNull(1)?.toIntOrNull() ?: start
                require(start in 1..100 && end in start..100) { "节次区间无效：$piece" }
                start to end
            }
        }.sortedBy { it.first }.toList()
        val merged = mutableListOf<Pair<Int, Int>>()
        intervals.forEach { range ->
            val last = merged.lastOrNull()
            if (last != null && range.first <= last.second + 1) merged[merged.lastIndex] = last.first to maxOf(last.second, range.second)
            else merged.add(range)
        }
        if (merged.isEmpty()) {
            val clock = Regex("(\\d{1,2}:\\d{2})-(\\d{1,2}:\\d{2})").find(text)
            if (clock != null) {
                val from = PeriodTimes.parse(clock.groupValues[1]) ?: error("开始时间无效")
                val to = PeriodTimes.parse(clock.groupValues[2]) ?: error("结束时间无效")
                require(to.isAfter(from)) { "结束时间必须晚于开始时间" }
                val periods = 1..(if (config.isEmpty()) 13 else config.size.coerceAtMost(16))
                val start = periods.lastOrNull { !PeriodTimes.start(config, it).isAfter(from) } ?: 1
                val end = periods.lastOrNull { PeriodTimes.start(config, it).isBefore(to.minusMinutes(10)) } ?: start
                merged.add(start to maxOf(start, end))
            }
        }
        return merged.ifEmpty { listOf(0 to 0) }.map { (start, end) -> TimeParts(day, start, end, weeks) }
    }
}
