package com.schedule.app.data

/** Weekly widgets retain their toolbar at usable heights; overflowing courses remain scrollable. */
data class WidgetContentLayout(val height: Float, val weekly: Boolean, val needed: Float, val tasks: Boolean) {
    val headerHeight = 36f
    val dateHeight = if (weekly && height >= 180f) 40f else 0f
    val captionHeight = if (height >= 160f) 18f else 0f
    val available = (height - 24f - headerHeight - 8f - dateHeight - captionHeight).coerceAtLeast(1f)
    val taskHeight = if (!weekly && tasks && height >= 250f && available - needed >= 40f) 24f else 0f
    val footer = height >= 300f && (weekly || available - taskHeight - needed >= 56f)
    val bodyHeight = (available - taskHeight - if (footer) 40f else 0f).coerceAtLeast(1f)
    val scrolls = needed > bodyHeight + .5f
    companion object {
        /** No fixed course capacity: every measured item remains in the collection. */
        fun needed(rows: List<Float>, gap: Float = 4f) = rows.sum() + gap * (rows.size - 1).coerceAtLeast(0)
        fun distribute(rows: List<Float>, available: Float, gap: Float = 4f): List<Float> {
            if (rows.isEmpty()) return rows
            val extra = ((available - needed(rows,gap)) / rows.size).coerceIn(0f, 6f)
            return rows.map { it + extra }
        }
    }
}
