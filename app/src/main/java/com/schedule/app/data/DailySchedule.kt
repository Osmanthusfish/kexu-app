package com.schedule.app.data

import com.schedule.app.model.Course
import java.time.LocalTime

/** Shared ordering: active classes, upcoming classes; completed classes never displace them. */
fun remainingCourses(courses: List<Course>, starts: List<String>, now: LocalTime): List<Course> =
    courses.filter { it.startPeriod in 1..16 && it.endPeriod in it.startPeriod..16 }
        .filter { now.isBefore(PeriodTimes.start(starts, it.endPeriod).plusMinutes(45)) }
        .sortedWith(compareBy<Course>(
            { if (!PeriodTimes.start(starts, it.startPeriod).isAfter(now)) 0 else 1 },
            { PeriodTimes.start(starts, it.startPeriod) }, { it.id }
        ))
