package com.schedule.app

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.schedule.app.data.ReminderScheduler
import com.schedule.app.data.Store
import com.schedule.app.ui.resolvePreset
import kotlinx.coroutines.delay

/**
 * 全屏响铃控制页：锁屏时由全屏通知拉起，或点通知进入；
 * 本身不再播放声音（声音统一由悬浮窗 / 通知频道的铃声负责），
 * 「停止」和「稍后提醒」都会顺带清掉通知与悬浮窗，保证只有一个关停点。
 */
class AlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }

        val id = intent.getStringExtra("id") ?: ""
        val label = intent.getStringExtra("label") ?: "课程提醒"
        val preset = Store(this).load().let { resolvePreset(it.theme, it.customAccent) }
        val c1 = Color(preset.accent.toArgb())
        val c2 = Color(preset.accent2.toArgb())

        setContent {
            MaterialTheme {
                AlarmRingUi(
                    label = label,
                    primary = c1,
                    secondary = c2,
                    onStop = { cleanupAndFinish() },
                    onSnooze = {
                        ReminderScheduler.snooze(this@AlarmRingActivity, id, label)
                        cleanupAndFinish()
                    }
                )
            }
        }
    }

    /** 统一关停：取消通知铃声 + 移除悬浮窗，然后关闭本页。 */
    private fun cleanupAndFinish() {
        intent.getStringExtra("id")?.let { rid ->
            try { NotificationManagerCompat.from(this).cancel(rid.hashCode()) } catch (e: Exception) { }
        }
        RingOverlay.dismiss(this)
        finish()
    }
}

@Composable
private fun AlarmRingUi(
    label: String,
    primary: Color,
    secondary: Color,
    onStop: () -> Unit,
    onSnooze: () -> Unit
) {
    var now by remember { mutableStateOf(java.time.LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) { now = java.time.LocalTime.now(); delay(1000) }
    }
    Box(
        Modifier.fillMaxSize().background(Brush.linearGradient(listOf(primary, secondary))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.22f), modifier = Modifier.size(64.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Alarm, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("课程提醒", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text(label, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(
                "%02d:%02d".format(now.hour, now.minute),
                color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(36.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White, contentColor = primary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Close, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("停止", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = onSnooze,
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 16.dp)
                ) {
                    Text("稍后提醒（10 分钟）", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
