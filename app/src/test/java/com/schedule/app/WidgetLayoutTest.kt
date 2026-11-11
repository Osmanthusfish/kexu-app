package com.schedule.app

import com.schedule.app.data.*
import com.schedule.app.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.json.JSONObject
import java.io.File

class WidgetLayoutTest {
    @get:Rule val temp=TemporaryFolder()
    @Test fun sixDailyCoursesCanFitWithoutACapacityLimit() {
        val rows=List(6){32f};val needed=WidgetContentLayout.needed(rows)
        val layout=WidgetContentLayout(303f,false,needed,true)
        assertFalse(layout.scrolls);assertFalse(layout.footer);assertEquals(0f,layout.taskHeight)
        assertEquals(6,WidgetContentLayout.distribute(rows,layout.bodyHeight).size)
    }
    @Test fun denseContentKeepsItsHeightAndScrollsRatherThanDroppingCourses() {
        val rows=List(12){80f};val needed=WidgetContentLayout.needed(rows)
        val layout=WidgetContentLayout(303f,false,needed,true)
        assertTrue(layout.scrolls);assertFalse(layout.footer);assertEquals(0f,layout.taskHeight)
        assertEquals(rows,WidgetContentLayout.distribute(rows,layout.bodyHeight))
    }
    @Test fun secondaryActionsOnlyUseSpareSpace() {
        val layout=WidgetContentLayout(303f,false,90f,true)
        assertTrue(layout.footer);assertEquals(24f,layout.taskHeight);assertFalse(layout.scrolls)
    }
    @Test fun fullWeekDoesNotAllocateBlankPeriodRows() {
        val layout=WidgetContentLayout(460f,true,WidgetContentLayout.needed(listOf(90f,100f,60f)),false)
        assertFalse(layout.scrolls);assertTrue(layout.bodyHeight>=258f)
    }
    @Test fun smallSizesKeepPositiveContentSpaceWithoutSecondaryControls() {
        for(weekly in listOf(false,true))for(height in listOf(80f,100f,145f,180f,220f)) {
            val layout=WidgetContentLayout(height,weekly,250f,true)
            assertTrue(layout.bodyHeight>0);assertFalse(layout.footer);assertEquals(0f,layout.taskHeight)
        }
    }
    @Test fun extraSpaceIsBoundedToKeepCardsCompact() {
        val rows=listOf(40f,50f,60f)
        assertEquals(listOf(46f,56f,66f),WidgetContentLayout.distribute(rows,900f))
        assertEquals(158f,WidgetContentLayout.needed(rows))
        assertEquals(emptyList<Float>(),WidgetContentLayout.distribute(emptyList(),300f))
    }
    @Test fun dailyToolbarNeverCausesPreviouslyFittingContentToOverflow() {
        for(height in 80..700 step 10)for(needed in 0..800 step 10) {
            val layout=WidgetContentLayout(height.toFloat(),false,needed.toFloat(),true)
            if(needed<=layout.available)assertFalse(layout.scrolls)
        }
    }
    @Test fun weeklyToolbarRemainsVisibleForDenseAndExpandedSchedules() {
        for(height in listOf(303f,460f,618f)) {
            val layout=WidgetContentLayout(height,true,1000f,false)
            assertTrue(layout.footer);assertTrue(layout.scrolls);assertTrue(layout.bodyHeight>0)
        }
    }
    @Test fun allMaterialsRoundTripWithoutChangingCoursesOrTasks() {
        val old=BackupCodec.emptyData()
        for(style in 0..6) {
            val data=old.copy(visual=old.visual.copy(widgetStyle=style))
            assertEquals(data,BackupCodec.decode(BackupCodec.encode(BackupPayload(data,null,null))).data)
        }
    }
    @Test fun schemaThreeUpgradePreservesExactOldBytesAndEveryOlderRecovery() {
        val old=JSONObject(BackupCodec.encode(BackupPayload(BackupCodec.emptyData(),null,null))).put("schemaVersion",3).toString(2)
        File(temp.root,"data.json").writeText(old)
        val prior=File(temp.root,"recovery/before-v144-upgrade.json").apply { parentFile.mkdirs();writeText("older backup") }
        val store=Store(temp.root);store.save(store.load().copy(visual=VisualPreferences(widgetStyle=4)))
        store.save(store.load().copy(theme=3))
        assertEquals(old,store.exportUpgradeSnapshot());assertEquals("older backup",prior.readText())
        assertEquals(3,JSONObject(store.exportUpgradeSnapshot()).getInt("schemaVersion"))
    }
}
