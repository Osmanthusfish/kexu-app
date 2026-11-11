package com.schedule.app.data

import java.time.LocalTime

/** Shared by import, display and reminders; existing 1–13 defaults are preserved. */
object PeriodTimes {
    private val defaults = listOf("08:00", "08:55", "10:00", "10:55", "14:00", "14:55", "16:00", "16:50", "19:00", "19:50", "20:45", "21:40", "22:30")
    fun parse(value: String): LocalTime? = value.trim().takeIf { Regex("\\d{1,2}:\\d{2}").matches(it) }
        ?.let { runCatching { LocalTime.parse(it.padStart(5, '0')) }.getOrNull() }
    fun default(p: Int): LocalTime = defaults.getOrNull(p - 1)?.let(LocalTime::parse)
        ?: LocalTime.of(8, 0).plusMinutes((p - 1) * 50L)
    fun start(config: List<String>, p: Int): LocalTime = config.getOrNull(p - 1)?.let(::parse) ?: default(p)
    fun validate(values: List<String>): String? {
        val parsed = values.map { parse(it) ?: return "请填写有效时间（例如 08:00）" }
        if (parsed.zipWithNext().any { (a, b) -> !b.isAfter(a) }) return "每节开始时间必须晚于前一节；第 14～16 节请按学校时间填写"
        return null
    }
}
