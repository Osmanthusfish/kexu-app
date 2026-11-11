package com.schedule.app.data

import java.time.DayOfWeek
import java.time.LocalDate

data class ScreenshotTodoDraft(
    val title: String,
    val dueDate: LocalDate?,
    val dueTime: String?,
    val sourceDateText: String?,
    val needsDateConfirmation: Boolean,
    val recognizedText: String
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
        slashMonthDay.findAll(normalized).forEach { m ->
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
            val offset = if (m.value.startsWith("下")) (if (next == 0) 7 else next) + 7 else if (next == 0) 7 else next
            today.plusDays(offset.toLong()) to m.value
        }

        val selected = explicit.firstOrNull()?.let { Triple(it.second, it.first, explicit.size != 1 || !it.first.startsWith("20")) }
            ?: relative?.let { Triple(it.first, it.second, false) }
            ?: weekday?.let { Triple(it.first, it.second, true) }
        val time = time24.find(normalized)?.let { "%02d:%02d".format(it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
            ?: chineseTime.find(normalized)?.let { m ->
                var hour = m.groupValues[2].toInt()
                val period = m.groupValues[1]
                if ((period == "下午" || period == "晚上") && hour < 12) hour += 12
                if (period == "上午" && hour == 12) hour = 0
                val minute = if (m.groupValues[3] == "" && m.value.contains("半")) 30 else m.groupValues[3].toIntOrNull() ?: 0
                if (hour in 0..23 && minute in 0..59) "%02d:%02d".format(hour, minute) else null
            }

        val candidateLine = normalized.lineSequence().map(String::trim).firstOrNull { it.isNotBlank() } ?: ""
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
            recognizedText = text
        )
    }
}
