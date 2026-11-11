package com.schedule.app.ui

import androidx.compose.ui.graphics.Color

/**
 * 内置主题。c1/c2 是全屏背景渐变的两端色，accent/accent2 是整套界面
 * （顶栏按钮、今日卡、FAB、图标等）跟随变化的主题色。
 * index 0 = 默认「云白」。
 */
data class ThemePreset(
    val name: String,
    val c1: Color,      // 背景渐变顶部
    val c2: Color,      // 背景渐变底部
    val accent: Color,  // 主色
    val accent2: Color, // 主色渐变的另一端
)

val PRESETS = listOf(
    ThemePreset("云白", Color(0xFFE5F1F9), Color(0xFFF7FBFE), Color(0xFF3F80B2), Color(0xFF75AECF)),
    ThemePreset("湖蓝", Color(0xFFA6CDF7), Color(0xFFE4F1FE), Color(0xFF2E7BD6), Color(0xFF57A2EC)),
    ThemePreset("薄荷", Color(0xFFA5E3C6), Color(0xFFE9F8F0), Color(0xFF1F9E68), Color(0xFF4FC48F)),
    ThemePreset("蜜桃", Color(0xFFFFC9A0), Color(0xFFFFF2E7), Color(0xFFEE8245), Color(0xFFF4A976)),
    ThemePreset("薰衣草", Color(0xFFCDB7F5), Color(0xFFF4EEFF), Color(0xFF8460DD), Color(0xFFAB8DEE)),
    ThemePreset("日落", Color(0xFFFFB49E), Color(0xFFFFEFE8), Color(0xFFE15D6E), Color(0xFFF0869A)),
)

/** theme 值 -> 主题配置；0 也返回默认主题，任何值都不会为空。 */
fun presetFor(index: Int): ThemePreset = PRESETS.getOrNull(index.coerceAtLeast(0)) ?: PRESETS[0]

/** theme==6 时使用用户自选的主题色，从主色派生整套渐变。 */
const val THEME_CUSTOM = 6

/** 统一入口：按 themeId 与自定义主色解析出实际主题；其余值回落到内置主题。 */
fun resolvePreset(themeId: Int, customAccent: Int): ThemePreset {
    if (themeId == THEME_CUSTOM && customAccent != -1) {
        val a = Color(customAccent).copy(alpha = 1f)
        return ThemePreset("自定义", lighten(a, 0.72f), lighten(a, 0.90f), a, lighten(a, 0.30f))
    }
    return presetFor(themeId)
}

/** 变暗：f=0 不变，f=1 全黑。用于从主题色派生深色文字/图案。 */
fun darken(c: Color, f: Float): Color =
    Color(c.red * (1 - f), c.green * (1 - f), c.blue * (1 - f), c.alpha)

/** 变亮（往白色混）：f=0 不变，f=1 全白。 */
fun lighten(c: Color, f: Float): Color =
    Color(c.red + (1 - c.red) * f, c.green + (1 - c.green) * f, c.blue + (1 - c.blue) * f, c.alpha)
