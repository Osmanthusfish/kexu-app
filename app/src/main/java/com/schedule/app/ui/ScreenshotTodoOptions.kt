package com.schedule.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.schedule.app.model.Semester
import org.json.JSONObject

/** Optional task metadata is kept separate from OCR; no course is guessed. */
data class ScreenshotTodoOptions(val semesterId: String? = null, val courseId: String? = null, val remindMinutes: Int? = null) {
    fun encode(): String = JSONObject().put("semester", semesterId ?: "").put("course", courseId ?: "")
        .put("remind", remindMinutes ?: -1).toString()
    companion object {
        fun decode(encoded: String): ScreenshotTodoOptions = JSONObject(encoded).let {
            ScreenshotTodoOptions(it.optString("semester").ifBlank { null }, it.optString("course").ifBlank { null }, it.optInt("remind", -1).takeIf { v -> v >= 0 })
        }
    }
}

@Composable
fun ScreenshotTodoOptionFields(options: ScreenshotTodoOptions, semesters: List<Semester>, hasDate: Boolean, enabled: Boolean, onChange: (ScreenshotTodoOptions) -> Unit) {
    var coursePicker by rememberSaveable { mutableStateOf(false) }
    var remindMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var notificationsGranted by remember { mutableStateOf(Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> notificationsGranted = granted }
    val courseName = semesters.firstOrNull { it.id == options.semesterId }?.courses?.firstOrNull { it.id == options.courseId }?.name
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        OutlinedButton(enabled = enabled, modifier = Modifier.fillMaxWidth(), onClick = { coursePicker = true }) {
            Text(courseName?.let { "关联课程：$it" } ?: if (options.courseId != null) "关联课程已不存在，请重选" else "关联课程（可选）")
        }
        Box {
            OutlinedButton(enabled = enabled, onClick = { remindMenu = true }) {
                Text(options.remindMinutes?.let { if (it == 0) "截止时提醒" else if (it == 1440) "提前 1 天提醒" else if (it == 10080) "提前 1 周提醒" else "提前 $it 分钟提醒" } ?: "未设置提醒")
            }
            DropdownMenu(expanded = remindMenu, onDismissRequest = { remindMenu = false }) {
                listOf(null to "不提醒", 0 to "截止时提醒", 5 to "提前 5 分钟", 10 to "提前 10 分钟", 30 to "提前 30 分钟", 60 to "提前 1 小时", 1440 to "提前 1 天", 10080 to "提前 1 周").forEach { (minutes, label) ->
                    DropdownMenuItem(text = { Text(label) }, enabled = minutes == null || hasDate, onClick = {
                        remindMenu = false
                        onChange(options.copy(remindMinutes = minutes))
                        if (minutes != null && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    })
                }
            }
        }
        if (!hasDate && options.remindMinutes != null) Text("提醒需要截止日期，请补充日期或关闭提醒。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        else if (options.remindMinutes != null) {
            Text("未填截止时间时按 23:59 计算；已过提醒时间不会补发。", style = MaterialTheme.typography.bodySmall)
            if (!notificationsGranted) Text("通知权限未开启，提醒可能无法显示。请在系统设置中允许通知。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            if (Build.VERSION.SDK_INT >= 31 && !context.getSystemService(android.app.AlarmManager::class.java).canScheduleExactAlarms()) {
                Text("未开启精确闹钟权限，系统可能延迟提醒。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { runCatching { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:${context.packageName}"))) } }) { Text("设置准时提醒") }
            }
        }
    }
    if (coursePicker) AlertDialog(onDismissRequest = { coursePicker = false }, title = { Text("关联课程") }, text = {
        Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            TextButton(onClick = { onChange(options.copy(semesterId = null, courseId = null)); coursePicker = false }) { Text("不关联课程") }
            if (semesters.isEmpty()) Text("暂无课程，可稍后编辑待办关联。")
            semesters.forEach { sem ->
                Text(sem.title, style = MaterialTheme.typography.titleSmall)
                sem.courses.forEach { course -> TextButton(onClick = {
                    onChange(options.copy(semesterId = sem.id, courseId = course.id)); coursePicker = false
                }, modifier = Modifier.fillMaxWidth()) { Text(course.name) } }
            }
        }
    }, confirmButton = { TextButton(onClick = { coursePicker = false }) { Text("关闭") } })
}
