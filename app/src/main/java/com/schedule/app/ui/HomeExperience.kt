package com.schedule.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.schedule.app.*
import com.schedule.app.data.*
import java.time.LocalDate
import java.time.ZoneId

private fun LayoutCoordinates.screenRectOrNull(): Rect? {
    val point = localToScreen(Offset.Zero)
    return if (point.x.isFinite() && point.y.isFinite()) Rect(point, Size(size.width.toFloat(), size.height.toFloat())) else null
}

/** Today is a timeline; the full timetable is always one tap away. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun HomeExperience(vm: AppViewModel) {
    val now = LocalAppTime.current
    val today = now.toLocalDate()
    val sem = pickDisplaySemester(vm.semesters, vm.homeSemesterId, today)
    var selected by rememberSaveable(today.toString()) { mutableStateOf(today.toString()) }
    val date = LocalDate.parse(selected)
    val courses = sem?.let { OccurrenceRules.onDate(vm.data, it, date) }.orEmpty()
    val starts = sem?.periodStarts?.ifEmpty { vm.data.periodStarts } ?: vm.data.periodStarts
    val nowZoned = now.atZone(ZoneId.systemDefault())
    val focus = remember(vm.data, sem, nowZoned) { sem?.let { OccurrenceRules.upcoming(vm.data, it, nowZoned, 1).firstOrNull() } }
    val duration = motionDuration(340)
    var quickCreate by rememberSaveable { mutableStateOf(false) }
    var detailDate by rememberSaveable { mutableStateOf<String?>(null) }
    var detailKey by rememberSaveable { mutableStateOf<String?>(null) }
    var detailSource by remember { mutableStateOf<Rect?>(null) }
    var detailSourceStatus by remember { mutableStateOf<String?>(null) }
    var editingCourse by rememberSaveable { mutableStateOf(false) }
    val detailOccurrence = sem?.let { semester ->
        detailDate?.let { Wk.parseDateOrNull(it) }?.let { day ->
            OccurrenceRules.onDate(vm.data, semester, day).firstOrNull { it.key == detailKey }
        }
    }
    fun openDetail(o: CourseOccurrence, source: Rect?, status: String?) { detailSource = source; detailSourceStatus = status; detailDate = o.date.toString(); detailKey = o.key; editingCourse = false }
    fun closeDetail() { detailDate = null; detailKey = null; detailSource = null; detailSourceStatus = null; editingCourse = false }
    if (sem != null && detailOccurrence != null) {
        val base = detailOccurrence.base
        if (!editingCourse) CourseDetails(vm, sem, base, detailOccurrence, onDismiss = { closeDetail() }, onEdit = { editingCourse = true }, sourceBounds = detailSource, sourceStatus = detailSourceStatus)
        else CourseDialog(course = base, editable = true,
            periodStarts = effectivePeriodStarts(sem, vm.data.periodStarts),
            onDismiss = { closeDetail() },
            onDelete = { id -> vm.upsertSemester(sem.copy(courses = sem.courses.filterNot { it.id == id })); closeDetail() },
            onConfirm = { changed -> vm.upsertSemester(sem.copy(courses = sem.courses.map { if (it.id == changed.id) changed else it })); closeDetail() })
    }
    if(quickCreate)QuickCreateSheet(vm){quickCreate=false}
    Scaffold(containerColor = Color.Transparent, bottomBar = {
        Surface(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
            shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
            Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf(Triple("课表", Icons.Default.ViewWeek, Screen.Schedule), Triple("待办", Icons.Default.CheckCircle, Screen.Todos),
                    Triple("小组件", Icons.Default.Widgets, Screen.Widgets), Triple("外观", Icons.Default.Palette, Screen.Theme)).forEach { (label, icon, screen) ->
                    Column(Modifier.weight(1f).pressMotion().clip(RoundedCornerShape(22.dp)).clickable {
                        if (screen == Screen.Schedule && sem != null) vm.go(screen, sem.id)
                        else vm.go(if (screen == Screen.Schedule) Screen.Import else screen)
                    }.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(icon, null, Modifier.size(21.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(3.dp)); Text(label, color=MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${today.monthValue} 月 ${today.dayOfMonth} 日 · ${weekdayFull(today.dayOfWeek.value)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("我的课表", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                }
                FilledTonalIconButton(onClick = { quickCreate=true }, modifier = Modifier.size(48.dp).pressMotion()) { Icon(Icons.Default.Add, "快速新建") }
            }
            DayRibbon(today,date,{d->sem!=null && OccurrenceRules.onDate(vm.data,sem,d).isNotEmpty()},{selected=it.toString()})
            Reveal { FocusCourse(vm, sem, focus, detailKey == focus?.key && detailSource != null && !editingCourse, ::openDetail) }
            Reveal(1) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (date == today) "今日安排" else "${date.monthValue} 月 ${date.dayOfMonth} 日", fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { if (sem != null) vm.openRoute("day", sem.id, date = date.toString()) else vm.go(Screen.Import) }) { Text("${courses.size} 次课  ↗") }
                    }
                    AnimatedContent(selected, transitionSpec = {
                        val direction=if(targetState>initialState)1 else -1
                        (slideInHorizontally(tween(duration)){it/5*direction}+fadeIn(tween(duration))) togetherWith
                            (slideOutHorizontally(tween(duration/2)){-it/8*direction}+fadeOut(tween(duration/2))) using SizeTransform(sizeAnimationSpec={_,_->tween(duration)})
                    }, label = "daily agenda") {
                        val entries = sem?.let { s -> OccurrenceRules.onDate(vm.data, s, LocalDate.parse(it)) }.orEmpty()
                        val remaining = entries.filterNot { entry -> entry.key == focus?.key }
                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            if (entries.isEmpty()) Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .72f)) {
                                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.WbSunny, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp))
                                    Text(if (sem == null) "导入课表，开启新的学期" else "没有课程，留一点时间给自己。", fontSize = 13.sp)
                                }
                            }
                            if (entries.isNotEmpty() && remaining.isEmpty()) {
                                Text("当天课程已在上方展示", modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                            remaining.take(3).forEach { o ->
                                var cardBounds by remember(o.key) { mutableStateOf<Rect?>(null) }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.width(47.dp)) {
                                        Text(PeriodTimes.start(starts, o.course.startPeriod).toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(PeriodTimes.start(starts, o.course.endPeriod).plusMinutes(45).toString(), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Surface(onClick = { openDetail(o, cardBounds, null) },
                                        modifier = Modifier.weight(1f).onGloballyPositioned { cardBounds = it.screenRectOrNull() }
                                            .alpha(if (detailKey == o.key && detailSource != null && !editingCourse) 0f else 1f).pressMotion(), shape = RoundedCornerShape(19.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .92f)) {
                                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.width(3.dp).height(34.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .6f), CircleShape))
                                            Column(Modifier.padding(start = 11.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Text(o.course.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text("${o.course.room.ifBlank { "地点未填" }} · ${o.course.teacher.ifBlank { "教师未填" }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }
                            if (remaining.size > 3) TextButton(onClick = { vm.openRoute("day", sem!!.id, date = it) }, modifier = Modifier.fillMaxWidth()) { Text("查看当天全部 ${entries.size} 次课程") }
                        }
                    }
                }
            }
            Reveal(2) { Column { TodoCard(vm); NextCountdownCard(vm) } }
            SectionTitle("我的学期")
            if (vm.semesters.isEmpty()) OutlinedButton(onClick = { vm.go(Screen.Import) }, modifier = Modifier.fillMaxWidth()) { Text("导入第一份课表") }
            else vm.semesters.sortedByDescending { it.startDate }.forEach { SemesterCard(vm, it) }
            SectionTitle("工具与设置")
            QuickActions(vm)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable private fun FocusCourse(vm: AppViewModel, sem: com.schedule.app.model.Semester?, o: CourseOccurrence?, hiddenForTransition: Boolean, onOpen: (CourseOccurrence, Rect?, String?) -> Unit) {
    val now = LocalAppTime.current.atZone(ZoneId.systemDefault())
    val starts = sem?.periodStarts?.ifEmpty { vm.data.periodStarts } ?: vm.data.periodStarts
    val from = o?.let { PeriodTimes.start(starts, it.course.startPeriod) }
    val until = o?.let { PeriodTimes.start(starts, it.course.endPeriod).plusMinutes(45) }
    val active = o != null && !now.isBefore(OccurrenceRules.startsAt(o, starts, now.zone)) && now.isBefore(OccurrenceRules.endsAt(o, starts, now.zone))
    val accent = MaterialTheme.colorScheme.primary
    var cardBounds by remember(o?.key) { mutableStateOf<Rect?>(null) }
    val status = when { o == null -> "给时间一点留白"; active -> "正在上课"; o.date == now.toLocalDate() -> "下一节 · $from"; else -> "下一次 · ${o.date.monthValue}/${o.date.dayOfMonth} $from" }
    Box(Modifier.fillMaxWidth().onGloballyPositioned { cardBounds = it.screenRectOrNull() }.alpha(if (hiddenForTransition) 0f else 1f).pressMotion().clip(RoundedCornerShape(28.dp))
        .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface)))
        .clickable { if (o != null) onOpen(o, cardBounds, status) else vm.go(if (sem == null) Screen.Import else Screen.Schedule, sem?.id) }) {
        AuroraDecoration(Modifier.matchParentSize().alpha(.18f),accent)
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(accent, CircleShape)); Spacer(Modifier.width(7.dp))
                Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Icon(Icons.Default.NorthEast, null, Modifier.size(18.dp), tint = accent)
            }
            Text(o?.course?.name ?: if (sem == null) "从一份课表开始" else "接下来，享受空闲", color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
            if (o != null) {
                Text("${o.course.room.ifBlank { "地点未填写" }}  ·  ${o.course.teacher.ifBlank { "教师未填写" }}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                if(active) {
                    val begin=OccurrenceRules.startsAt(o,starts,now.zone)
                    val end=OccurrenceRules.endsAt(o,starts,now.zone)
                    val ratio=(java.time.Duration.between(begin,now).seconds.toFloat()/java.time.Duration.between(begin,end).seconds.coerceAtLeast(1)).coerceIn(0f,1f)
                    LinearProgressIndicator(progress={ratio},modifier=Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),color=accent,trackColor=accent.copy(alpha=.14f))
                } else HorizontalDivider(color = accent.copy(alpha = .16f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, Modifier.size(15.dp), tint = accent); Spacer(Modifier.width(6.dp))
                    Text("$from–$until", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text(periodLabel(o.course.startPeriod, o.course.endPeriod), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            } else Text(if (sem == null) "导入 Excel / CSV，安排每天的学习与生活。" else "点击查看整周课表", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}
