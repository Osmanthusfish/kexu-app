package com.schedule.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/** The detail surface grows from the tapped card; the Dialog retains modal focus and system back. */
@Composable
internal fun SkyCourseDialog(source: Rect, title: String, room: String, teacher: String, status: String?, onDismiss: () -> Unit, content: @Composable (() -> Unit) -> Unit) {
    val duration = motionDuration(320)
    val progress = remember(source) { Animatable(if (duration == 0) 1f else 0f) }
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }
    var surfaceWidth by remember { mutableIntStateOf(0) }
    var surfaceHeight by remember { mutableIntStateOf(0) }
    var screenOrigin by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current
    fun close() {
        if (closing) return
        closing = true
        scope.launch {
            if (duration == 0 || screenOrigin == null) progress.snapTo(0f)
            else progress.animateTo(0f, tween(duration / 2, easing = FastOutSlowInEasing))
            onDismiss()
        }
    }
    LaunchedEffect(surfaceWidth, surfaceHeight, duration, screenOrigin) {
        if (surfaceWidth > 0 && surfaceHeight > 0 && screenOrigin != null && !closing) {
            if (duration == 0) progress.snapTo(1f)
            else progress.animateTo(1f, tween(duration, easing = FastOutSlowInEasing))
        }
    }
    LaunchedEffect(source) {
        delay(250)
        if (screenOrigin == null) progress.snapTo(1f)
    }
    Dialog(onDismissRequest = { close() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned {
            val point = it.localToScreen(Offset.Zero)
            if (point.x.isFinite() && point.y.isFinite()) screenOrigin = point
        }, contentAlignment = Alignment.BottomCenter) {
            val viewportHeight = with(density) { maxHeight.toPx() }
            val viewportWidth = with(density) { maxWidth.toPx() }
            val maxSheetHeight = maxHeight * .90f
            val fallbackSheetHeight = with(density) { maxSheetHeight.toPx() }.coerceAtLeast(1f)
            val bottomPadding = with(density) { maxOf(WindowInsets.navigationBars.getBottom(this), 24.dp.roundToPx()).toDp() }
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(max = maxSheetHeight)
                    .onSizeChanged { surfaceWidth = it.width; surfaceHeight = it.height }
                    .graphicsLayer {
                        val f = progress.value.coerceIn(0f, 1f)
                        val width = if (surfaceWidth > 0) surfaceWidth.toFloat() else viewportWidth.coerceAtLeast(1f)
                        val height = if (surfaceHeight > 0) surfaceHeight.toFloat() else fallbackSheetHeight
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = (source.width / width).coerceIn(.02f, 2f) * (1f - f) + f
                        scaleY = (source.height / height).coerceIn(.02f, 2f) * (1f - f) + f
                        val origin = screenOrigin ?: Offset.Zero
                        val targetLeft = origin.x + (viewportWidth - width) / 2f
                        val targetTop = origin.y + viewportHeight - height
                        translationX = (source.left - targetLeft) * (1f - f)
                        translationY = (source.top - targetTop) * (1f - f)
                    },
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp,
                    bottomStart = (24f * (1f - progress.value)).dp, bottomEnd = (24f * (1f - progress.value)).dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp
            ) {
                Box {
                    Column(Modifier.padding(bottom = bottomPadding).graphicsLayer {
                        alpha = ((progress.value - .3f) / .5f).coerceIn(0f, 1f)
                    }) {
                        var drag by remember { mutableFloatStateOf(0f) }
                        Box(Modifier.fillMaxWidth().height(32.dp)
                            .semantics { contentDescription = "下滑关闭课程详情" }
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(onDragStart = { drag = 0f }, onDragEnd = {
                                    if (drag > 50.dp.toPx()) close()
                                }) { _, amount -> drag += amount }
                            }, contentAlignment = Alignment.Center) {
                            Box(Modifier.width(32.dp).height(4.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(2.dp)))
                        }
                        content(::close)
                    }
                    if (progress.value < .8f) {
                        val f = progress.value.coerceIn(0f, 1f)
                        val width = if (surfaceWidth > 0) surfaceWidth.toFloat() else viewportWidth.coerceAtLeast(1f)
                        val height = if (surfaceHeight > 0) surfaceHeight.toFloat() else fallbackSheetHeight
                        val sx = (source.width / width).coerceIn(.02f, 2f) * (1f - f) + f
                        val sy = (source.height / height).coerceIn(.02f, 2f) * (1f - f) + f
                        val sourceWidth = with(density) { source.width.toDp() }
                        Box(Modifier.matchParentSize().clearAndSetSemantics { }.pointerInput(Unit) {
                            awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } }
                        }.graphicsLayer { alpha = (1f - f / .8f).coerceIn(0f, 1f) }
                            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface)))) {
                            Column(Modifier.width(sourceWidth).graphicsLayer {
                                transformOrigin = TransformOrigin(0f, 0f)
                                scaleX = 1f / sx; scaleY = 1f / sy
                            }.padding(horizontal = 16.dp, vertical = if (status == null) 8.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (status != null) Text(status, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(title, fontSize = if (status == null) 14.sp else 24.sp, lineHeight = if (status == null) 19.sp else 30.sp,
                                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = if (status == null) 1 else 2, overflow = TextOverflow.Ellipsis)
                                Text("${room.ifBlank { "地点未填写" }} · ${teacher.ifBlank { "教师未填写" }}", fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
