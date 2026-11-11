package com.schedule.app.data

import java.time.DayOfWeek
import java.time.LocalDate

data class ScreenshotTodoDraft(
    val title: String,
    val dueDate: LocalDate?,
    val dueTime: String?,
    val sourceDateText: String?,
    val needsDateConfirmation: Boolean,
    val ambiguousTimeText: String?,
    val recognizedText: String,
    val multipleDateMentions: Boolean = false
)

/** Conservative text extraction: a date inferred from an incomplete expression must be confirmed. */
object ScreenshotTodoParser {
    private val iso = Regex("(?<!\\d)(20\\d{2})[-/.年](\\d{1,2})[-/.月](\\d{1,2})日?(?!\\d)")
    private val monthDay = Regex("(?<!\\d)(\\d{1,2})月(\\d{1,2})(?:日|号)?(?!\\d)")
    private val slashMonthDay = Regex("(?<!\\d)(\\d{1,2})/(\\d{1,2})(?!\\d)")
    private val time24 = Regex("(?<!\\d)([01]?\\d|2[0-3])[:：]([0-5]\\d)(?!\\d)")
    private val chineseTime = Regex("(上午|下午|晚上|中午)?\\s*(\\d{1,2})\\s*点(?:半|([0-5]\\d)分?)?")

    fun parse(text: String, today: LocalDate = LocalDate.now()): ScreenshotTodoDraft {
        val normalized = text.replace('０', '0').replace('１', '1').replace('２', '2')
            .replace('３', '3').replace('４', '4').replace('５', '5').replace('６', '6')
            .replace('７', '7').replace('８', '8').replace('９', '9')
        val explicit = mutableListOf<Pair<String, LocalDate>>()
        iso.findAll(normalized).forEach { m ->
            runCatching { LocalDate.of(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()) }
                .getOrNull()?.let { explicit += m.value to it }
        }
        val isoRanges = iso.findAll(normalized).map { it.range }.toList()
        monthDay.findAll(normalized).filterNot { m -> isoRanges.any { it.first <= m.range.first && it.last >= m.range.last } }.forEach { m ->
            runCatching { LocalDate.of(today.year, m.groupValues[1].toInt(), m.groupValues[2].toInt()) }
                .getOrNull()?.let { explicit += m.value to it }
        }
        slashMonthDay.findAll(normalized).filterNot { m -> isoRanges.any { it.first <= m.range.first && it.last >= m.range.last } }.forEach { m ->
            runCatching { LocalDate.of(today.year, m.groupValues[1].toInt(), m.groupValues[2].toInt()) }
                .getOrNull()?.let { explicit += m.value to it }
        }

        val relative = when {
            normalized.contains("大后天") -> today.plusDays(3) to "大后天"
            normalized.contains("后天") -> today.plusDays(2) to "后天"
            normalized.contains("明天") -> today.plusDays(1) to "明天"
            normalized.contains("今天") -> today to "今天"
            else -> null
        }
        val weekday = Regex("(?:下周|下星期|周|星期)([一二三四五六日天])").find(normalized)?.let { m ->
            val target = when (m.groupValues[1]) {
                "一" -> DayOfWeek.MONDAY; "二" -> DayOfWeek.TUESDAY; "三" -> DayOfWeek.WEDNESDAY
                "四" -> DayOfWeek.THURSDAY; "五" -> DayOfWeek.FRIDAY; "六" -> DayOfWeek.SATURDAY
                else -> DayOfWeek.SUNDAY
            }
            val next = (target.value - today.dayOfWeek.value + 7) % 7
            val offset = if (m.value.startsWith("下")) 7 - today.dayOfWeek.value + target.value else if (next == 0) 7 else next
            today.plusDays(offset.toLong()) to m.value
        }

        val relativeMentions = Regex("大后天|后天|明天|今天|下周[一二三四五六日天]|下星期[一二三四五六日天]|周[一二三四五六日天]|星期[一二三四五六日天]").findAll(normalized).count()
        val multipleDates = explicit.size + relativeMentions > 1
        val selected = explicit.firstOrNull()?.let { Triple(it.second, it.first, multipleDates || !it.first.startsWith("20")) }
            // Screenshot capture time is unknown: relative dates must never be treated as certain.
            ?: relative?.let { Triple(it.first, it.second, true) }
            ?: weekday?.let { Triple(it.first, it.second, true) }
        val clockMatches = time24.findAll(normalized).toList()
        val chineseMatches = chineseTime.findAll(normalized).toList()
        val multipleTimes = clockMatches.size + chineseMatches.size > 1
        val chineseMatch = chineseMatches.singleOrNull()
        val time = if (multipleTimes) null else clockMatches.singleOrNull()?.let { "%02d:%02d".format(it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
            ?: chineseMatch?.let { m ->
                var hour = m.groupValues[2].toInt()
                val period = m.groupValues[1]
                // A bare 1–12 o'clock could mean either morning or afternoon.
                if (period.isEmpty() && hour in 1..12) return@let null
                if (period.isNotEmpty() && hour !in 1..12) return@let null
                // These expressions are used inconsistently, so leave them to the user.
                if ((period == "中午" && hour != 12) || (period == "晚上" && hour == 12)) return@let null
                if ((period == "下午" || period == "晚上") && hour < 12) hour += 12
                if (period == "上午" && hour == 12) hour = 0
                val minute = if (m.groupValues[3] == "" && m.value.contains("半")) 30 else m.groupValues[3].toIntOrNull() ?: 0
                if (hour in 0..23 && minute in 0..59) "%02d:%02d".format(hour, minute) else null
            }

        // Chat screenshots commonly start with a sender or message timestamp, not the task.
        // Prefer a line containing an action; the result remains editable and is never auto-saved.
        val actionWords = Regex("提交|上交|完成|填写|报名|参加|缴费|交付|领取|发送|上传|下载|准备|携带|截止|作业|任务|报告|问卷|申请")
        val candidateLine = normalized.lineSequence().map(String::trim).filter { it.isNotBlank() }
            .maxByOrNull { line ->
                val actions = actionWords.findAll(line).count().coerceAtMost(3)
                actions * 10 + (if (line.length >= 6) 1 else 0)
            }.orEmpty()
        val title = candidateLine
            .replace(iso, " ").replace(monthDay, " ").replace(slashMonthDay, " ")
            .replace(time24, " ").replace(chineseTime, " ")
            .replace(Regex("(?:大后天|后天|明天|今天|下周[一二三四五六日天]|下星期[一二三四五六日天]|周[一二三四五六日天]|星期[一二三四五六日天])"), " ")
            .replace(Regex("^(截止日期|截止时间|截止|交|提交|上交|完成|作业|待办)\\s*[:：]?\\s*"), "")
            .replace(Regex("^(交|提交|上交|完成|截止)\\s*"), "")
            .replace(Regex("\\s+"), " ").trim(' ', '，', ',', '。', ';', '；', '、', ':', '：')

        return ScreenshotTodoDraft(
            title = title.ifBlank { candidateLine.take(80) },
            dueDate = selected?.first,
            dueTime = time,
            sourceDateText = selected?.second,
            needsDateConfirmation = selected?.third ?: true,
            ambiguousTimeText = if (multipleTimes) "识别到多个时间" else chineseMatch?.value?.takeIf { time == null },
            recognizedText = text,
            multipleDateMentions = multipleDates
        )
    }
}
