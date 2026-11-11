package com.schedule.app

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import com.schedule.app.data.ReminderScheduler
import com.schedule.app.data.Store
import com.schedule.app.ui.resolvePreset

/**
 * 跨应用悬浮响铃窗：在任意 App / 桌面上方醒目弹出，可「停止」或「稍后提醒（10 分钟）」。
 * 需要用户授予「显示在其他应用上层」权限（SYSTEM_ALERT_WINDOW）。
 */
object RingOverlay {

    private var root: LinearLayout? = null
    private var labelView: TextView? = null
    private var noteView: TextView? = null
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    /** 显示悬浮响铃窗。返回是否真的弹出成功（失败时调用方回退到通知响铃）。 */
    fun show(context: Context, id: String, label: String, note: String, withSound: Boolean = true): Boolean {
        return try {
            if (root != null) {
                // 已有窗在响：替换内容即可
                labelView?.text = label
                noteView?.text = if (note.isBlank()) "" else "备注：$note"
                if (withSound) startRing(context)
                true
            } else {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val views = buildView(context, id, label, note)
                root = views.root
                labelView = views.label
                noteView = views.note
                // 贴屏幕顶部、留出左右与上边距，不越出屏幕边框；带弹入动画
                val screenW = context.resources.displayMetrics.widthPixels
                val params = WindowManager.LayoutParams(
                    (screenW * 0.94f).toInt(),
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = dp(context, 24)
                    windowAnimations = R.style.RingOverlayAnimation
                }
                wm.addView(views.root, params)
                if (withSound) startRing(context)
                true
            }
        } catch (e: Exception) { false }
    }

    /** 停止响铃并移除悬浮窗。 */
    fun dismiss(context: Context) {
        try {
            root?.let {
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(it)
            }
        } catch (e: Exception) { }
        root = null
        labelView = null
        noteView = null
        stopRing()
    }

    private fun startRing(context: Context) {
        stopRing()
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (uri != null) {
                player = MediaPlayer.create(context.applicationContext, uri)?.also {
                    it.isLooping = true
                    it.setVolume(1f, 1f)
                    it.start()
                }
            }
        } catch (e: Exception) { }
        try {
            vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 600), 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 1000, 600), 0)
            }
        } catch (e: Exception) { }
    }

    private fun stopRing() {
        try { player?.stop() } catch (e: Exception) { }
        try { player?.release() } catch (e: Exception) { }
        player = null
        try { vibrator?.cancel() } catch (e: Exception) { }
        vibrator = null
    }

    private class Views(val root: LinearLayout, val label: TextView, val note: TextView)

    private fun buildView(context: Context, id: String, label: String, note: String): Views {
        val preset = Store(context).load().let { resolvePreset(it.theme, it.customAccent) }
        val c1 = preset.accent.toArgb()
        val c2 = preset.accent2.toArgb()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 24), dp(context, 18), dp(context, 24), dp(context, 18))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(c1, c2)).apply {
                cornerRadius = dpf(context, 24)
                setStroke(dp(context, 1), 0x66FFFFFF.toInt())
            }
            elevation = dpf(context, 10)
        }

        val badge = TextView(context).apply {
            text = "课程提醒 · 响铃中"
            textSize = 12f
            setTextColor(0xD9FFFFFF.toInt())
        }
        val tvLabel = TextView(context).apply {
            text = label
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, dp(context, 3), 0, 0)
        }
        val clock = TextClock(context).apply {
            format12Hour = "HH:mm"
            format24Hour = "HH:mm"
            textSize = 44f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFFFFFFFF.toInt())
        }
        val tvNote = TextView(context).apply {
            text = if (note.isBlank()) "" else "备注：$note"
            textSize = 12f
            setTextColor(0xBFFFFFFF.toInt())
        }

        val stop = pill(context, "停止", 0xF2FFFFFF.toInt(), 0xFF1C2230.toInt()) {
            dismiss(context)
            cancelAlarm(context, id)
        }
        val snooze = pill(context, "稍后提醒（10 分钟）", 0x33FFFFFF.toInt(), 0xFFFFFFFF.toInt()) {
            ReminderScheduler.snooze(context, id, label)
            dismiss(context)
            cancelAlarm(context, id)
        }
        stop.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { marginEnd = dp(context, 10) }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(context, 6), 0, 0)
        }
        row.addView(stop)
        row.addView(snooze)

        root.addView(badge)
        root.addView(tvLabel)
        root.addView(clock)
        root.addView(tvNote)
        root.addView(row)
        return Views(root, tvLabel, tvNote)
    }

    private fun pill(context: Context, text: String, bg: Int, fg: Int, onClick: () -> Unit): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(fg)
            setPadding(dp(context, 18), dp(context, 9), dp(context, 18), dp(context, 9))
            background = GradientDrawable().apply {
                cornerRadius = dpf(context, 24)
                setColor(bg)
            }
            isClickable = true
            setOnClickListener { onClick() }
        }

    /** 一起关掉通知铃声与响铃前台服务（悬浮窗自己不放音时声音全靠它们）。 */
    private fun cancelAlarm(context: Context, id: String) {
        try { androidx.core.app.NotificationManagerCompat.from(context).cancel(id.hashCode()) } catch (e: Exception) { }
        try { context.stopService(android.content.Intent(context, AlarmRingService::class.java)) } catch (e: Exception) { }
    }

    private fun dp(context: Context, v: Int): Int = (v * context.resources.displayMetrics.density).toInt()
    private fun dpf(context: Context, v: Int): Float = v * context.resources.displayMetrics.density
}
