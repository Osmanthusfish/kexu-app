package com.schedule.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography

private val TextDark = Color(0xFFE4E8F2)
private val TextDark2 = Color(0xFFA8B0C0)

/** 浅色 / 深色两套配色都从所选主题派生，保证换肤时两套模式都协调。 */
@Composable
fun ScheduleTheme(themeId: Int, dark: Boolean, customAccent: Int = -1, content: @Composable () -> Unit) {
    val p = resolvePreset(themeId, customAccent)
    val primary by animateColorAsState(if (dark) lighten(p.accent, 0.12f) else p.accent, tween(motionDuration(400)), label = "primary")
    val secondary by animateColorAsState(if (dark) lighten(p.accent2, 0.12f) else p.accent2, tween(motionDuration(400)), label = "secondary")
    val bgTop by animateColorAsState(if (dark) darken(p.c1, 0.84f) else p.c1, tween(motionDuration(400)), label = "bgTop")
    val bgBottom by animateColorAsState(if (dark) darken(p.c2, 0.80f) else p.c2, tween(motionDuration(400)), label = "bgBottom")

    val scheme = if (dark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF10141C),
            primaryContainer = darken(p.accent, 0.55f),
            onPrimaryContainer = lighten(p.accent, 0.72f),
            secondary = secondary,
            onSecondary = Color(0xFF10141C),
            secondaryContainer = darken(p.accent2, 0.55f),
            onSecondaryContainer = lighten(p.accent2, 0.72f),
            tertiary = secondary,
            onTertiary = Color(0xFF10141C),
            background = bgBottom,
            onBackground = TextDark,
            surface = Color(0xFF171B25),
            onSurface = TextDark,
            onSurfaceVariant = TextDark2,
            surfaceVariant = Color(0xFF232936),
            outline = Color(0xFF414B5C),
            outlineVariant = Color(0xFF2A3140),
            surfaceContainerLowest = Color(0xFF0F1219),
            surfaceContainerLow = Color(0xFF141822),
            surfaceContainer = Color(0xFF181D28),
            surfaceContainerHigh = Color(0xFF1E2430),
            surfaceContainerHighest = Color(0xFF242B39),
            error = Color(0xFFE08A8A),
        )
    } else {
        val darkAccent = darken(p.accent, 0.38f)
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = lighten(primary, 0.82f),
            onPrimaryContainer = darkAccent,
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = lighten(secondary, 0.80f),
            onSecondaryContainer = darken(p.accent, 0.30f),
            tertiary = secondary,
            onTertiary = Color.White,
            background = bgBottom,
            onBackground = Text0,
            surface = Color.White,
            onSurface = Text0,
            onSurfaceVariant = Text1,
            surfaceVariant = Color(0xFFE7ECF4),
            outline = Color(0xFFB9C2D2),
            outlineVariant = Color(0xFFE2E7EF),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF8FAFD),
            surfaceContainer = Color(0xFFF3F6FA),
            surfaceContainerHigh = Color(0xFFEDF1F7),
            surfaceContainerHighest = Color(0xFFE7ECF3),
            error = Color(0xFFD25A5A),
        )
    }
    MaterialTheme(colorScheme = scheme, shapes = Shapes(extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(30.dp)),
        typography = Typography(headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold), headlineMedium = TextStyle(fontSize = 25.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold), titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold), bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 24.sp), bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 21.sp)), content = content)
}

private val Text0 = Color(0xFF1B2130)
private val Text1 = Color(0xFF4A5160)
