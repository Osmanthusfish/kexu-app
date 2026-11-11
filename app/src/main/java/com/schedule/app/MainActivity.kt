package com.schedule.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.schedule.app.data.ReminderScheduler
import com.schedule.app.data.Store
import com.schedule.app.ui.AppBackdrop
import com.schedule.app.ui.BackupScreen
import com.schedule.app.ui.CountdownScreen
import com.schedule.app.ui.HomeScreen
import com.schedule.app.ui.ImportScreen
import com.schedule.app.ui.ReminderScreen
import com.schedule.app.ui.ScheduleTheme
import com.schedule.app.ui.SemesterScreen
import com.schedule.app.ui.ThemeScreen
import com.schedule.app.ui.TodoScreen
import com.schedule.app.ui.WallpaperEditOverlay

class MainActivity : ComponentActivity() {

    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /** activity 级 ViewModel：桌面小组件快捷入口要在 onCreate/onNewIntent 期间用它导航。 */
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 沉浸式：背景渐变一直铺到状态栏/导航栏后面，状态栏图标用深色（背景始终是浅色）
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        ReminderReceiver.ensureChannel(this)
        handleWidgetOpen(intent)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val dark = when (vm.darkMode) { 2 -> true; 1 -> false; else -> systemDark }
            // 应用内字体缩放：在主题外层罩一层 Compose 官方密度覆盖。
            // 只改 fontScale（文字），不改 density（布局）；默认 1.0 = 与系统一致，零影响。
            val baseDensity = androidx.compose.ui.platform.LocalDensity.current
            val now = com.schedule.app.ui.rememberAppTime()
            androidx.compose.runtime.CompositionLocalProvider(
                com.schedule.app.ui.LocalMotionLevel provides if(android.animation.ValueAnimator.areAnimatorsEnabled()) 2 else 0,
                com.schedule.app.ui.LocalAppTime provides now,
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                    baseDensity.density,
                    (baseDensity.fontScale * vm.fontScale).coerceIn(0.6f, 2.0f)
                )
            ) {
                ScheduleTheme(themeId = vm.theme, dark = dark, customAccent = vm.customAccent) {
                    val view = androidx.compose.ui.platform.LocalView.current
                    if (!view.isInEditMode) androidx.compose.runtime.SideEffect {
                        androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                            isAppearanceLightStatusBars = !dark
                            isAppearanceLightNavigationBars = !dark
                        }
                    }
                    Box(Modifier.fillMaxSize()) {
                        AppBackdrop(vm, dark)
                        if (vm.isLoading) {
                            androidx.compose.material3.CircularProgressIndicator(Modifier.align(androidx.compose.ui.Alignment.Center))
                        } else androidx.compose.runtime.CompositionLocalProvider(
                            androidx.compose.material3.LocalContentColor provides androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                        ) { ScreenRoot(vm) }
                        if (vm.isSaving) {
                            androidx.compose.material3.LinearProgressIndicator(Modifier.align(androidx.compose.ui.Alignment.TopCenter))
                        }
                        vm.errorMessage?.let { message ->
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { vm.dismissError() },
                                title = { androidx.compose.material3.Text("数据保存提示") },
                                text = { androidx.compose.material3.Text(message) },
                                confirmButton = {
                                    androidx.compose.material3.TextButton(onClick = { vm.dismissError() }) {
                                        androidx.compose.material3.Text("知道了")
                                    }
                                }
                            )
                        }
                        // 全屏壁纸裁剪层：盖在所有页面之上，与主界面同一窗口坐标系，所见即所得
                        vm.wallpaperEditUri?.let { uri ->
                            WallpaperEditOverlay(
                                uri = uri,
                                onDismiss = { vm.closeWallpaperEdit() },
                                onConfirm = { s, x, y ->
                                    vm.setBgFull(uri, s, x, y)
                                    vm.closeWallpaperEdit()
                                }
                            )
                        }
                        // 小组件壁纸裁剪层：窄长预览框，框内所见即小组件卡片背景
                        vm.widgetWallpaperEditUri?.let { uri ->
                            WallpaperEditOverlay(
                                uri = uri,
                                widgetAspect = CourseWidgetProvider.PREVIEW_ASPECT,
                                onDismiss = { vm.closeWidgetWallpaperEdit() },
                                onConfirm = { s, x, y ->
                                    vm.setWidgetBgFull(uri, s, x, y)
                                    vm.closeWidgetWallpaperEdit()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetOpen(intent)
    }

    /** 桌面小组件快捷按钮：带着 open=xxx 打开对应功能页（课表/闹钟/待办）。 */
    private fun handleWidgetOpen(intent: android.content.Intent) {
        val entry = intent.getStringExtra("open") ?: return
        vm.openRoute(entry, intent.getStringExtra("semester"), intent.getStringExtra("course"), intent.getStringExtra("date"), intent.getStringExtra("todo"), intent.getStringExtra("occurrence"))
        listOf("open", "semester", "course", "date", "todo", "occurrence").forEach { intent.removeExtra(it) }
    }

    @Composable
    private fun ScreenRoot(vm: AppViewModel) {
        // 系统返回键 = 返回上一级。放在会随导航重组的作用域里，canBack 才能实时生效
        BackHandler(enabled = vm.canBack) { vm.back() }
        val order = Screen.entries.withIndex().associate { it.value to it.index }
        val duration = com.schedule.app.ui.motionDuration(280)
        AnimatedContent(
            targetState = vm.screen,
            transitionSpec = {
                val forward = (order[targetState] ?: 0) >= (order[initialState] ?: 0)
                val sign=if(forward)1 else -1
                val move=tween<androidx.compose.ui.unit.IntOffset>(duration,easing=FastOutSlowInEasing)
                ((slideInHorizontally(move){sign*it/6}+fadeIn(tween(duration*2/3))+scaleIn(tween(duration),initialScale=.985f)) togetherWith
                    (slideOutHorizontally(move){-sign*it/12}+fadeOut(tween(duration/2))+scaleOut(tween(duration),targetScale=.99f)))
                    .using(SizeTransform(clip=false,sizeAnimationSpec={_,_->tween(duration)}))
            },
            label = "nav"
        ) { s ->
            when (s) {
                Screen.Home -> HomeScreen(vm)
                Screen.Schedule -> SemesterScreen(vm)
                Screen.Import -> ImportScreen(vm)
                Screen.Reminders -> ReminderScreen(vm)
                Screen.Theme -> ThemeScreen(vm)
                Screen.Countdown -> CountdownScreen(vm)
                Screen.Backup -> BackupScreen(vm)
                Screen.Todos -> TodoScreen(vm)
                Screen.Widgets -> com.schedule.app.ui.WidgetStudioScreen(vm)
            }
        }
    }

}
