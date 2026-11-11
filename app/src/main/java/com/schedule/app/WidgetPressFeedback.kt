package com.schedule.app

/** Consistent gentle native press feedback for every widget, including legacy preferences. */
internal object WidgetPressFeedback {
    private val courses = intArrayOf(
        R.drawable.widget_course_0, R.drawable.widget_course_1, R.drawable.widget_course_2,
        R.drawable.widget_course_3, R.drawable.widget_course_4, R.drawable.widget_course_5
    )
    fun chip() = R.drawable.widget_chip
    fun day() = R.drawable.widget_day_selected
    fun sky(selected: Boolean) = if (selected) R.drawable.widget_sky_day_selected else R.drawable.widget_sky_day
    fun transparent() = R.drawable.widget_press_gentle
    fun course(index: Int) = courses[index.coerceIn(0, courses.lastIndex)]
}
