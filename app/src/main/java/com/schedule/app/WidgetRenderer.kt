package com.schedule.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.*
import android.text.style.*
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.schedule.app.data.*
import com.schedule.app.model.*
import com.schedule.app.ui.*
import androidx.compose.ui.graphics.toArgb
import java.time.ZonedDateTime

data class WidgetColors(val top:Int,val bottom:Int,val text:Int,val muted:Int,val accent:Int)

/** All interactive content remains native RemoteViews, including individual overlap cards. */
object WidgetRenderer {
    fun colors(context:Context,data:AppData):WidgetColors {
        val accent=resolvePreset(data.theme,data.customAccent).accent.toArgb()
        return when(data.visual.widgetStyle) {
            1 -> WidgetColors(0xFF243D5B.toInt(),0xFF101C32.toInt(),0xFFF3F5FF.toInt(),0xFFB4C7DE.toInt(),0xFF9FDDFF.toInt())
            2 -> WidgetColors(androidx.core.graphics.ColorUtils.blendARGB(accent,Color.BLACK,.30f),androidx.core.graphics.ColorUtils.blendARGB(accent,Color.BLACK,.64f),Color.WHITE,0xFFD6E0F4.toInt(),0xFFD4DFFF.toInt())
            3 -> WidgetColors(0xFFFFD0B1.toInt(),0xFFF6B8D3.toInt(),0xFF542E47.toInt(),0xFF865773.toInt(),0xFFA83B64.toInt())
            4 -> WidgetColors(0xFFACE1D1.toInt(),0xFFDCEBB9.toInt(),0xFF173F3A.toInt(),0xFF436C61.toInt(),0xFF176B5B.toInt())
            5 -> WidgetColors(0xFFD4BFF4.toInt(),0xFFF2D4EA.toInt(),0xFF3F2959.toInt(),0xFF745986.toInt(),0xFF7543AE.toInt())
            6 -> WidgetColors(0xFFFAFCFF.toInt(),0xFFEDF0F5.toInt(),0xFF1C2942.toInt(),0xFF65758E.toInt(),0xFF394EA8.toInt())
            else -> WidgetColors(0xFFF9FCFF.toInt(),0xFFFFFFFF.toInt(),0xFF19384F.toInt(),0xFF526C7F.toInt(),0xFF326F9D.toInt())
        }
    }
    fun page(context:Context,data:AppData,id:Int,weekly:Boolean,size:SizeF,state:WidgetBrowseState,previous:WidgetBrowseState?=null):RemoteViews {
        val content=FullWidgetRenderer.page(context,data,id,weekly,size,state)
        return content.remote.apply { FullWidgetRenderer.bindCollection(context,this,content.rows,id,weekly,size) }
    }
}
