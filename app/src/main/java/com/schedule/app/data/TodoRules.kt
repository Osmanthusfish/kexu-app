package com.schedule.app.data

import com.schedule.app.model.Todo
import java.time.*

object TodoRules {
    fun deadline(todo: Todo, zone: ZoneId = ZoneId.systemDefault()): ZonedDateTime? = todo.dueDate?.let {
        LocalDate.parse(it).atTime(LocalTime.parse(todo.dueTime ?: "23:59")).atZone(zone)
    }
    fun nextMillis(todo: Todo, now: ZonedDateTime = ZonedDateTime.now()): Long {
        if (todo.done || todo.remindMinutesBefore == null) return -1
        val fire = deadline(todo, now.zone)?.minusMinutes(todo.remindMinutesBefore.toLong()) ?: return -1
        return if (fire.isAfter(now)) fire.toInstant().toEpochMilli() else -1
    }
}
