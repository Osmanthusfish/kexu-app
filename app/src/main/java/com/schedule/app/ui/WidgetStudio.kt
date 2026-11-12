package com.schedule.app.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.app.*
import com.schedule.app.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun WidgetStudioScreen(vm:AppViewModel) {
    var weekly by rememberSaveable { mutableStateOf(false) }
    var sizeIndex by rememberSaveable { mutableIntStateOf(1) }
    var font by remember(vm.data.visual.widgetFontScale) { mutableFloatStateOf(vm.data.visual.widgetFontScale) }
    var opacity by remember(vm.data.visual.widgetOpacity) { mutableFloatStateOf(vm.data.visual.widgetOpacity) }
    var pinMessage by remember { mutableStateOf<String?>(null) }
    val ctx=LocalContext.current
    val wallpaperPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            vm.openWidgetWallpaperEdit(uri.toString())
        }
    }
    val prefs=vm.data.visual.copy(widgetFontScale=font,widgetOpacity=opacity)
    Scaffold(containerColor=Color.Transparent,topBar={TopAppBar(title={Text("小组件工作室")},colors=topBarTransparent(),navigationIcon={IconButton(onClick={vm.back()}){Icon(Icons.AutoMirrored.Filled.ArrowBack,"返回")}})}) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text("把桌面，变成你的。",fontSize=22.sp,fontWeight=FontWeight.Bold)
            Text("颜色、密度与信息，即刻预览",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                listOf("极光","曜石","主题","日落","薄荷","雾紫","月白").forEachIndexed { index,label ->
                    val palette=WidgetRenderer.colors(ctx,vm.data.copy(visual=prefs.copy(widgetStyle=index)))
                    val selected=index==prefs.widgetStyle
                    Column(Modifier.weight(1f).pressMotion(.92f).clip(RoundedCornerShape(14.dp)).selectable(selected,role=Role.RadioButton,onClick={vm.updateVisual { it.copy(widgetStyle=index) }}),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(palette.top),Color(palette.bottom))))
                            .border(if(selected)2.dp else 1.dp,if(selected)MaterialTheme.colorScheme.primary else Color.White.copy(alpha=.5f),CircleShape),contentAlignment=Alignment.Center) {
                            androidx.compose.animation.AnimatedVisibility(selected,enter=scaleIn(tween(motionDuration(280)))+fadeIn(tween(motionDuration(180))),exit=fadeOut(tween(motionDuration(120)))) {
                                Icon(Icons.Default.Check,null,Modifier.size(19.dp),tint=Color(palette.text))
                            }
                        }
                        Text(label,fontSize=10.sp,fontWeight=if(selected)FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            SlidingTabs(listOf("今日时间轴","整周课表"),if(weekly)1 else 0,{weekly=it==1})
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("尺寸预览",fontSize=12.sp,modifier=Modifier.weight(1f))
                listOf("紧凑","标准","展开").forEachIndexed { index,label ->
                    TextButton({sizeIndex=index},contentPadding=PaddingValues(horizontal=8.dp),modifier=Modifier.height(36.dp)) {
                        Text(label,fontSize=11.sp,color=if(index==sizeIndex)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,fontWeight=if(index==sizeIndex)FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            Text("尺寸仅供预览；添加后的大小由手机桌面决定。可尝试长按桌面组件并拖动边框调整，具体取决于桌面支持情况。",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.fillMaxWidth().animateContentSize(tween(motionDuration(360)))) { LiveWidgetPreview(vm.data.copy(visual=prefs),weekly,sizeIndex,vm.wallpaperRevision,vm.widgetPreviews) }
            Text(if(weekly)"整周按天完整显示，空白时段不占行。课量特别多时上下滑动，保持字号清楚。" else "按时间显示今天全部课程，已结束也保留。拉高组件可一屏看全，小尺寸可上下滑动。",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            FilledTonalButton(onClick={
                val manager=AppWidgetManager.getInstance(ctx)
                pinMessage=if(manager.isRequestPinAppWidgetSupported) {
                    if(manager.requestPinAppWidget(ComponentName(ctx,if(weekly)AgendaWidgetProvider::class.java else CourseWidgetProvider::class.java),null,null))
                        "请在桌面确认添加；添加后可尝试长按组件调整大小。"
                    else "桌面未接受添加请求，请长按桌面空白处 → 小组件 → 课序，选择添加。"
                } else "请长按桌面空白处 → 小组件 → 课序，选择添加。"
            },modifier=Modifier.fillMaxWidth().pressMotion()) {Icon(Icons.Filled.Add,null);Spacer(Modifier.width(8.dp));Text("添加到桌面")}
            pinMessage?.let{Text(it,fontSize=12.sp,color=MaterialTheme.colorScheme.primary)}
            StudioCard("字体与通透感") {
                Text("组件字号 · ${(font*100).toInt()}%",style=MaterialTheme.typography.labelLarge)
                Slider(font,{font=it},valueRange=.8f..1.3f,steps=4,onValueChangeFinished={vm.updateVisual { current -> current.copy(widgetFontScale=font) }})
                Text("背景不透明度 · ${(opacity*100).toInt()}%",style=MaterialTheme.typography.labelLarge)
                Slider(opacity,{opacity=it},valueRange=.5f..1f,steps=4,onValueChangeFinished={vm.updateVisual { current -> current.copy(widgetOpacity=opacity) }})
                Text("七套材质用于两个组件。",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StudioCard("小组件壁纸") {
                Text("独立于应用壁纸，可拖动和缩放调整。",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Button(onClick={wallpaperPicker.launch(arrayOf("image/*"))}) { Text("选择壁纸") }
                    OutlinedButton(onClick={vm.setWidgetBg(null)}) { Text("恢复默认") }
                }
            }
            StudioCard("显示哪些信息") {
                StudioSwitch("教室",prefs.widgetShowRoom){vm.updateVisual { current -> current.copy(widgetShowRoom=it) }}
                StudioSwitch("教师",prefs.widgetShowTeacher){vm.updateVisual { current -> current.copy(widgetShowTeacher=it) }}
                StudioSwitch("今日组件中的待办",prefs.widgetShowTasks){vm.updateVisual { current -> current.copy(widgetShowTasks=it) }}
                StudioSwitch("保留空白周末列",prefs.widgetWeekends){vm.updateVisual { current -> current.copy(widgetWeekends=it) }}
                Text("周末有课时仍会显示，避免漏掉课程。",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick={vm.setVisual(VisualPreferences())}){Text("恢复默认外观")}
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable private fun StudioCard(title:String,content:@Composable ColumnScope.()->Unit) {
    Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface.copy(alpha=.92f),tonalElevation=1.dp) {
        Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text(title,fontWeight=FontWeight.Bold,fontSize=16.sp);Spacer(Modifier.height(2.dp));content()
        }
    }
}
@Composable private fun StudioSwitch(label:String,checked:Boolean,onChange:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {Text(label,Modifier.weight(1f));Switch(checked,onChange)}
}
