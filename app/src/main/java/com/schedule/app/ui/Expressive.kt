package com.schedule.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.schedule.app.*
import com.schedule.app.data.*
import java.time.LocalDate

/** One spring-driven selection surface, rather than seven independently flashing pills. */
@Composable fun SlidingTabs(labels:List<String>,selected:Int,onSelect:(Int)->Unit) {
    val motion=LocalMotionLevel.current
    val spec:FiniteAnimationSpec<Dp> = if(motion==0)tween(0) else spring(dampingRatio=if(motion==1)1f else .78f,stiffness=450f)
    BoxWithConstraints(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(17.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha=.75f)).padding(4.dp)) {
        val cell=maxWidth/labels.size
        val x by animateDpAsState(cell*selected,spec,label="tab surface")
        Box(Modifier.offset(x=x).width(cell).fillMaxHeight().background(MaterialTheme.colorScheme.primary,RoundedCornerShape(13.dp)))
        Row(Modifier.fillMaxSize()) {labels.forEachIndexed { index,label ->
            val fg by animateColorAsState(if(index==selected)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,tween(motionDuration(180)),label="tab ink")
            Box(Modifier.weight(1f).fillMaxHeight().pressMotion(.95f).clip(RoundedCornerShape(13.dp))
                .selectable(index==selected,role=Role.Tab,onClick={onSelect(index)}),contentAlignment=Alignment.Center) {
                Text(label,color=fg,fontSize=12.sp,fontWeight=if(index==selected)FontWeight.Bold else FontWeight.Medium)
            }
        }}
    }
}

@Composable fun DayRibbon(today:LocalDate,selected:LocalDate,hasCourse:(LocalDate)->Boolean,onSelect:(LocalDate)->Unit) {
    val motion=LocalMotionLevel.current
    val monday=Wk.mondayOf(today)
    val selectedIndex=java.time.temporal.ChronoUnit.DAYS.between(monday,selected).toInt().coerceIn(0,6)
    BoxWithConstraints(Modifier.fillMaxWidth().height(74.dp).clip(RoundedCornerShape(24.dp))
        .background(MaterialTheme.colorScheme.surface.copy(alpha=.68f)).padding(4.dp)) {
        val cell=maxWidth/7
        val x by animateDpAsState(cell*selectedIndex,if(motion==0)tween(0) else spring(dampingRatio=if(motion==1)1f else .72f,stiffness=430f),label="day ribbon")
        Box(Modifier.offset(x=x).width(cell).fillMaxHeight().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary,MaterialTheme.colorScheme.tertiary)),RoundedCornerShape(20.dp)))
        Row(Modifier.fillMaxSize()) {(0..6).forEach { index ->
            val date=monday.plusDays(index.toLong());val chosen=date==selected
            val fg by animateColorAsState(if(chosen)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,tween(motionDuration(180)),label="day ink")
            Column(Modifier.weight(1f).fillMaxHeight().pressMotion(.93f).clip(RoundedCornerShape(20.dp))
                .selectable(chosen,role=Role.Tab,onClick={onSelect(date)}),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                Text(weekdayChar(index+1),color=fg.copy(alpha=.7f),fontSize=10.sp,lineHeight=12.sp)
                Text(date.dayOfMonth.toString(),color=fg,fontWeight=FontWeight.Bold,fontSize=18.sp,lineHeight=22.sp)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.size(4.dp).background(if(hasCourse(date))fg else Color.Transparent,CircleShape))
            }
        }}
    }
}

/** A finite reveal: no continuous background animation, polling or extra widget refreshes. */
@Composable fun AuroraDecoration(modifier:Modifier=Modifier,accent:Color=MaterialTheme.colorScheme.primary) {
    val progress=remember { Animatable(0f) };val duration=motionDuration(1100)
    LaunchedEffect(duration) { if(duration==0)progress.snapTo(1f) else progress.animateTo(1f,tween(duration,easing=FastOutSlowInEasing)) }
    Canvas(modifier) {
        val t=progress.value
        drawRect(Brush.radialGradient(listOf(accent.copy(alpha=.8f),Color.Transparent),Offset(size.width*(1.15f-t*.2f),size.height*.1f),size.width*.95f))
        drawRect(Brush.radialGradient(listOf(Color(0xFF72D5EA).copy(alpha=.22f),Color.Transparent),Offset(size.width*.05f,size.height*(1.3f-t*.2f)),size.width*.7f))
        val center=Offset(size.width*.94f,size.height*.9f)
        for(i in 0..2) drawCircle(Color.White.copy(alpha=.08f),size.width*(.26f+i*.13f)*(.85f+t*.15f),center,style=Stroke(1.dp.toPx()))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun QuickCreateSheet(vm:AppViewModel,onDismiss:()->Unit) {
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(topStart=32.dp,topEnd=32.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal=22.dp).padding(bottom=28.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("安排下一件事",fontSize=24.sp,fontWeight=FontWeight.Bold)
            Text("从这里，开始你的计划。",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(Triple("记一项作业",Icons.Default.EditNote,"new-todo"),Triple("设置提醒",Icons.Default.NotificationsActive,"new-reminder"),Triple("导入课表",Icons.Default.FileOpen,"import")).forEachIndexed { index,(label,icon,route) ->
                Reveal(index) {
                    Surface(onClick={onDismiss();if(route=="import")vm.go(Screen.Import) else vm.openRoute(route)},modifier=Modifier.fillMaxWidth().pressMotion(.96f),shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.primaryContainer.copy(alpha=.5f)) {
                        Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                            Icon(icon,null,tint=MaterialTheme.colorScheme.primary)
                            Text(label,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
                            Icon(Icons.Default.NorthEast,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable fun CompletionMeter(done:Int,total:Int) {
    val progress by animateFloatAsState(if(total==0)0f else done.toFloat()/total,spatialSpring(),label="completion orbit")
    val accent=MaterialTheme.colorScheme.primary
    val duration=motionDuration(280)
    Surface(shape=RoundedCornerShape(25.dp),color=MaterialTheme.colorScheme.primaryContainer.copy(alpha=.55f),modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
            Box(Modifier.size(58.dp),contentAlignment=Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawArc(accent.copy(alpha=.13f),-90f,360f,false,style=Stroke(5.dp.toPx(),cap=StrokeCap.Round))
                    drawArc(Brush.sweepGradient(listOf(accent,Color(0xFF66B8C4),accent)),-90f,(progress*360f).coerceIn(0f,360f),false,style=Stroke(5.dp.toPx(),cap=StrokeCap.Round))
                }
                AnimatedContent(done,transitionSpec={slideInVertically(tween(duration)){it/2}+fadeIn(tween(duration)) togetherWith slideOutVertically(tween(duration/2)){-it/2}+fadeOut(tween(duration/2))},label="completed count") { Text(it.toString(),fontSize=24.sp,fontWeight=FontWeight.Bold) }
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(if(total>0 && done==total)"全部完成，做得不错。" else "一步一步，完成计划",fontSize=17.sp,fontWeight=FontWeight.Bold)
                Text("已完成 $done / $total 项",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
