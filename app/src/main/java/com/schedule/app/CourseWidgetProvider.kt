package com.schedule.app

import android.appwidget.*
import android.content.*
import android.graphics.*
import android.util.SizeF
import android.widget.RemoteViews
import com.schedule.app.data.*

/** Provider identities are unchanged, so existing home-screen placements survive upgrades. */
class CourseWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = refresh(context)
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) = refresh(context)
    override fun onDisabled(context: Context) = refresh(context)
    override fun onDeleted(context: Context, ids: IntArray) = WidgetNavigation.forget(context,ids)
    private fun refresh(context: Context) {
        val pending=goAsync()
        Thread { try { updateAll(context) } finally { pending.finish() } }.start()
    }
    companion object {
        const val PREVIEW_ASPECT = 2.5f
        private val updateLock=Any()
        fun updateAll(context: Context, animatedWidget: Int = -1, previous: WidgetBrowseState? = null) = synchronized(updateLock) {
            val manager=AppWidgetManager.getInstance(context)
            val daily=manager.getAppWidgetIds(ComponentName(context,CourseWidgetProvider::class.java))
            val weekly=manager.getAppWidgetIds(ComponentName(context,AgendaWidgetProvider::class.java))
            if(daily.isEmpty() && weekly.isEmpty()) { WidgetRefreshReceiver.cancel(context);return@synchronized }
            val data=Store(context).load()
            for((ids,isWeek) in listOf(daily to false,weekly to true)) for(id in ids) {
                val options=manager.getAppWidgetOptions(id)
                val minW=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,300).coerceAtLeast(160)
                val minH=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,if(isWeek)250 else 100).coerceAtLeast(80)
                val maxW=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,minW).coerceAtLeast(minW)
                val maxH=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,if(isWeek)500 else 250).coerceAtLeast(minH)
                val state=WidgetNavigation.read(context,id)
                fun render(size:SizeF):RemoteViews {
                    val shell=createShell(context,data,size)
                    shell.removeAllViews(R.id.widgetSwitcher)
                    val content=FullWidgetRenderer.page(context,data,id,isWeek,size,state)
                    shell.addView(R.id.widgetSwitcher,content.remote)
                    FullWidgetRenderer.bindCollection(context,shell,content.rows,id,isWeek,size)
                    shell.setInt(R.id.widgetSwitcher,"setDisplayedChild",0)
                    shell.setOnClickPendingIntent(android.R.id.background,AppRoutes.pending(context,"home"))
                    return shell
                }
                val portrait=SizeF(minW.toFloat(),maxH.toFloat());val landscape=SizeF(maxW.toFloat(),minH.toFloat())
                val views=if(android.os.Build.VERSION.SDK_INT>=31) {
                    val sizes=androidx.core.os.BundleCompat.getParcelableArrayList(options,AppWidgetManager.OPTION_APPWIDGET_SIZES,SizeF::class.java)
                        ?.filter{it.width>=1 && it.height>=1}?.distinct()?.take(4).orEmpty().ifEmpty{listOf(portrait,landscape).distinct()}
                    RemoteViews(sizes.associateWith(::render))
                } else RemoteViews(render(landscape),render(portrait))
                manager.updateAppWidget(id,views)
                if(android.os.Build.VERSION.SDK_INT<31)manager.notifyAppWidgetViewDataChanged(id,R.id.widgetList)
            }
            WidgetRefreshReceiver.schedule(context,data)
        }
        /** Shared by launcher and the in-app preview, including wallpaper and transparency. */
        private fun createShell(context: Context, data: com.schedule.app.model.AppData, size: SizeF): RemoteViews {
            val shell=RemoteViews(context.packageName,R.layout.widget_shell)
            val palette=WidgetRenderer.colors(context,data)
            val scale=512f/maxOf(size.width,size.height)
            val w=(size.width*scale).toInt().coerceAtLeast(1);val h=(size.height*scale).toInt().coerceAtLeast(1)
            val wallpaper=data.widgetBgImageUri?.let { runCatching { drawWallpaperCard(context,w,h,it,data.widgetBgScale,data.widgetBgOffX,data.widgetBgOffY) }.getOrNull() }
            val background=wallpaper ?: Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
            val canvas=Canvas(background)
            val paint=Paint(Paint.ANTI_ALIAS_FLAG)
            paint.shader=LinearGradient(0f,0f,w.toFloat(),h.toFloat(),palette.top,palette.bottom,Shader.TileMode.CLAMP)
            paint.alpha=if(wallpaper==null)255 else 205
            canvas.drawRect(0f,0f,w.toFloat(),h.toFloat(),paint)
            val skyStyle = data.visual.widgetStyle == 0
            paint.shader=RadialGradient(w*.9f,h*.08f,w*.72f,intArrayOf(androidx.core.graphics.ColorUtils.setAlphaComponent(palette.accent,if(skyStyle) 24 else 90),Color.TRANSPARENT),null,Shader.TileMode.CLAMP)
            paint.alpha=255;canvas.drawRect(0f,0f,w.toFloat(),h.toFloat(),paint)
            paint.shader=RadialGradient(w*.1f,h*.8f,w*.85f,intArrayOf(if(skyStyle) 0x187CCFFF else 0x507CCFFF,Color.TRANSPARENT),null,Shader.TileMode.CLAMP)
            canvas.drawRect(0f,0f,w.toFloat(),h.toFloat(),paint)
            paint.shader=null;paint.color=0x38FFFFFF;paint.style=Paint.Style.STROKE;paint.strokeWidth=1.5f
            canvas.drawRoundRect(RectF(1f,1f,w-1f,h-1f),24*scale,24*scale,paint)
            shell.setImageViewBitmap(R.id.widgetBg,background)
            shell.setInt(R.id.widgetBg,"setImageAlpha",(data.visual.widgetOpacity*255).toInt())
            return shell
        }
        internal fun preview(context: Context, data: com.schedule.app.model.AppData, weekly: Boolean, size: SizeF): WidgetPreviewContent {
            val content=FullWidgetRenderer.page(context,data,-1,weekly,size,WidgetBrowseState(Wk.mondayOf(java.time.LocalDate.now()),0),preview=true)
            val shell=createShell(context,data,size).apply {
                removeAllViews(R.id.widgetSwitcher);addView(R.id.widgetSwitcher,content.remote)
                setInt(R.id.widgetSwitcher,"setDisplayedChild",0)
            }
            FullWidgetRenderer.bindCollection(context,shell,content.rows,-1,weekly,size,preview=true)
            return content.copy(remote=shell)
        }
        /** 小组件壁纸卡片：与主界面同一套缩放/偏移公式平铺，再叠深色蒙版保证白字可读。 */
        private fun drawWallpaperCard(
            context: Context, w: Int, h: Int,
            uri: String, scale: Float, offX: Float, offY: Float
        ): Bitmap? {
            val bmp = decodeWidgetWallpaper(context, uri) ?: return null
            val card = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val cv = Canvas(card)
            val rad = minOf(w, h) * 0.16f
            val clip = Path().apply {
                addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), rad, rad, Path.Direction.CW)
            }
            cv.clipPath(clip)
            val sc = maxOf(w / bmp.width.toFloat(), h / bmp.height.toFloat()) * scale
            val dw = bmp.width * sc
            val dh = bmp.height * sc
            val cx = w / 2f + offX * w
            val cy = h / 2f + offY * h
            cv.drawBitmap(bmp, null, RectF(cx - dw / 2, cy - dh / 2, cx + dw / 2, cy + dh / 2), Paint(Paint.FILTER_BITMAP_FLAG))
            // 深色蒙版：上浅下深，文字始终清晰
            cv.drawRect(
                0f, 0f, w.toFloat(), h.toFloat(),
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, 0f, 0f, h.toFloat(),
                        0x4D000000.toInt(), 0x8C000000.toInt(), Shader.TileMode.CLAMP
                    )
                }
            )
            val sw = maxOf(1.5f, w * 0.004f)
            val hairline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = sw
                color = android.graphics.Color.WHITE
                alpha = 40
            }
            cv.drawRoundRect(RectF(sw, sw, w - sw, h - sw), rad, rad, hairline)
            return card
        }

        /** 解码小组件壁纸（私有文件或 content URI），按最大边降采样。 */
        private fun decodeWidgetWallpaper(context: Context, uri: String, maxDim: Int = 1200): Bitmap? {
            fun open(): java.io.InputStream? =
                if (uri == Store.WIDGET_WALLPAPER_MARKER) {
                    Store.widgetWallpaperFile(context).takeIf { it.exists() }?.inputStream()
                } else {
                    context.contentResolver.openInputStream(android.net.Uri.parse(uri))
                }

            val opt = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            open()?.use { s -> BitmapFactory.decodeStream(s, null, opt) }
            if (opt.outWidth <= 0) return null
            var sample = 1
            while (opt.outWidth / (sample * 2) >= maxDim || opt.outHeight / (sample * 2) >= maxDim) sample *= 2
            return open()?.use { s ->
                val bmp = BitmapFactory.decodeStream(s, null, BitmapFactory.Options().apply { inSampleSize = sample })
                if (bmp == null || bmp.width <= 0) null else bmp
            }
        }

    }
}
