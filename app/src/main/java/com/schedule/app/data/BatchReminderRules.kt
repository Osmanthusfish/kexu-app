package com.schedule.app.data

import com.schedule.app.model.*
import java.util.UUID

object BatchReminderRules {
    /** Existing individual exceptions are preserved unless the user explicitly requests replacement. */
    fun apply(data: AppData, semesterId: String, courseIds: Set<String>, lead: Int, replaceExisting: Boolean): AppData {
        require(lead in 0..10080)
        val sem = data.semesters.find { it.id == semesterId } ?: return data
        var reminders = data.reminders
        sem.courses.filter { it.id in courseIds && it.weekday in 1..7 && it.startPeriod in 1..16 }.forEach { c ->
            val existing = reminders.filter { it.linkedSemesterId == sem.id && it.linkedCourseId == c.id }
            if (existing.isEmpty()) {
                val time = PeriodTimes.start(sem.periodStarts.ifEmpty { data.periodStarts }, c.startPeriod)
                reminders = reminders + Reminder(UUID.randomUUID().toString(), c.name, c.weekday, time.hour, time.minute,
                    lead, c.room, true, c.startPeriod, sem.id, c.id)
            } else if (replaceExisting) reminders = reminders.map { if (it in existing) it.copy(leadMinutes = lead, enabled = true) else it }
        }
        return data.copy(reminders = reminders)
    }
}
