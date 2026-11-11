package com.schedule.app.ui

import android.content.Context
import android.content.res.Configuration
import android.appwidget.AppWidgetHostView
import android.util.SizeF
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViews
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ListView
import com.schedule.app.WidgetPreviewContent
import com.schedule.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.schedule.app.CourseWidgetProvider
import com.schedule.app.model.AppData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

internal data class WidgetPreviewKey(val data: AppData, val weekly: Boolean, val width: Float,
    val height: Float, val wallpaperRevision: Long, val minute: LocalDateTime, val configuration: Configuration)

/** ViewModel-owned, never holds an Activity or a View. Three previews bound bitmap memory usage. */
internal class WidgetPreviewCache {
    private val renderLock = Mutex()
    private val entries = object : LinkedHashMap<WidgetPreviewKey, WidgetPreviewContent>(3, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<WidgetPreviewKey, WidgetPreviewContent>?) = size > 3
    }
    @Synchronized fun get(key: WidgetPreviewKey): WidgetPreviewContent? = entries[key]
    @Synchronized private fun put(key: WidgetPreviewKey, value: WidgetPreviewContent) { entries[key] = value }
    suspend fun render(context: Context, key: WidgetPreviewKey): WidgetPreviewContent = renderLock.withLock {
        get(key) ?: withContext(Dispatchers.IO) {
            val result = CourseWidgetProvider.preview(context.applicationContext, key.data, key.weekly, SizeF(key.width, key.height))
            ensureActive() // Superseded slider values must not publish an obsolete preview.
            put(key, result)
            result
        }
    }
}

@Composable internal fun LiveWidgetPreview(data: AppData, weekly: Boolean, sizeIndex: Int,
    wallpaperRevision: Long, cache: WidgetPreviewCache) {
    val ctx = LocalContext.current
    val configuration = Configuration(LocalConfiguration.current)
    val minute = LocalAppTime.current.withSecond(0).withNano(0)
    val height = (if (weekly) listOf(303, 460, 618) else listOf(145, 303, 410))[sizeIndex].dp
    Surface(shape = RoundedCornerShape(24.dp), color = androidx.compose.ui.graphics.Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            val width = if (sizeIndex == 0) minOf(maxWidth, if (weekly) 250.dp else 180.dp) else maxWidth
            val key = WidgetPreviewKey(data, weekly, width.value, height.value, wallpaperRevision, minute, configuration)
            // Retain the last good preview while sliders are changing; cancellation prevents stale results winning.
            var failed by remember(key) { mutableStateOf(false) }
            val remote by produceState(cache.get(key), key) {
                try { value = cache.render(ctx, key) }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { failed = true; android.util.Log.w("WidgetPreview", "Preview rendering failed", e) }
            }
            Box(Modifier.width(width).height(height).clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer), contentAlignment = Alignment.Center) {
                if (remote == null) Text(if (failed) "预览暂时不可用，请重新进入" else "正在生成预览", style = MaterialTheme.typography.bodySmall)
                AndroidView(factory = { PreviewFrame(it) }, modifier = Modifier.fillMaxSize(),
                    onRelease = { it.release() }, update = { frame -> remote?.let(frame::show) })
            }
        }
    }
}

/** The public widget host API owns async inflation, cancellation and safe reapplication. */
private class PreviewFrame(context: Context) : AppWidgetHostView(context) {
    private var request: WidgetPreviewContent? = null
    init {
        setExecutor(Dispatchers.IO.asExecutor())
        android.appwidget.AppWidgetManager.getInstance(context).installedProviders
            .firstOrNull { it.provider.packageName==context.packageName && it.provider.className==CourseWidgetProvider::class.java.name }
            ?.let { setAppWidget(-1,it) }
        setPadding(0, 0, 0, 0)
    }
    fun show(remote: WidgetPreviewContent) {
        if (request === remote) return
        request = remote
        updateAppWidget(remote.remote)
    }
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val payload = request
        fun disable(v: View) {
            if (v is AdapterView<*>) {
                v.onItemClickListener = null
                if (android.os.Build.VERSION.SDK_INT < 31 && v is ListView && v.id == R.id.widgetList && payload != null && payload.rows.isNotEmpty() && v.tag !== payload) {
                    v.tag = payload
                    v.adapter = object : BaseAdapter() {
                        override fun getCount() = payload.rows.size
                        override fun getItem(position: Int) = payload.rows[position]
                        override fun getItemId(position: Int) = position.toLong()
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = getItem(position).apply(context, parent)
                    }
                }
            } else { v.setOnClickListener(null); v.isClickable = false }
            if (v is ViewGroup) for (i in 0 until v.childCount) disable(v.getChildAt(i))
        }
        disable(this) // Preview buttons cannot launch app routes or change the real launcher widget.
    }
    fun release() { request = null; updateAppWidget(null) }
}
