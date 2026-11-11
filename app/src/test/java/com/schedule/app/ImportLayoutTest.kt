package com.schedule.app

import com.schedule.app.data.*
import com.schedule.app.model.Course
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.*
import org.junit.Test

class ImportLayoutTest {
    private fun zip(vararg entries: Pair<String, String>): ByteArray = ByteArrayOutputStream().also { output ->
        ZipOutputStream(output).use { zip -> entries.forEach { (name, text) -> zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray()); zip.closeEntry() } }
    }.toByteArray()
    private fun sheet(cell: String) = "<worksheet><sheetData><row r=\"1\">$cell</row></sheetData></worksheet>"

    @Test fun xlsxSupportsInlineAndRichSharedText() {
        val bytes = zip("xl/worksheets/sheet1.xml" to sheet("<c r=\"A1\" t=\"inlineStr\"><is><t>课程名称</t></is></c><c r=\"C1\" t=\"s\"><v>0</v></c>"),
            "xl/sharedStrings.xml" to "<sst><si><r><t>高等</t></r><r><t>数学</t></r></si></sst>")
        assertEquals(listOf(listOf("课程名称", "", "高等数学")), XlsxReader.read(bytes.inputStream()))
    }
    @Test fun xlsxUsesWorkbookOrderAndExposesAllSheets() {
        val bytes = zip("xl/worksheets/sheet2.xml" to sheet("<c r=\"A1\"><v>2</v></c>"),
            "xl/worksheets/sheet1.xml" to sheet("<c r=\"A1\"><v>1</v></c>"),
            "xl/workbook.xml" to "<workbook xmlns:r=\"urn:r\"><sheets><sheet name=\"秋\" r:id=\"a\"/><sheet name=\"春\" r:id=\"b\"/></sheets></workbook>",
            "xl/_rels/workbook.xml.rels" to "<Relationships><Relationship Id=\"a\" Target=\"worksheets/sheet1.xml\"/><Relationship Id=\"b\" Target=\"/xl/worksheets/sheet2.xml\"/></Relationships>")
        val sheets = XlsxReader.readSheets(bytes.inputStream())
        assertEquals(listOf("秋", "春"), sheets.map { it.name })
        assertEquals("1", sheets.first().rows.first().first())
    }
    @Test fun xlsxRejectsExternalEntitiesAndOversizedColumns() {
        for (xml in listOf("<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///private'>]><worksheet/>", sheet("<c r=\"ZZZ1\"><v>1</v></c>"))) {
            assertTrue(runCatching { XlsxReader.read(zip("xl/worksheets/sheet1.xml" to xml).inputStream()) }.isFailure)
        }
    }
    @Test fun pastedTablesPreserveCommasAndEscapedPipes() {
        val md = "|课程名称|上课时间|教师|\n|---|---|---|\n|数学\\|A|1-4,6-9周星期一1-2节|张,李|"
        assertEquals(listOf("数学|A", "1-4,6-9周星期一1-2节", "张,李"), CsvReader.readText(md)[1])
        assertEquals(listOf("数学", "1-4,6-9周", "张,李"), CsvReader.readText("课程名称\t上课时间\t教师\n数学\t1-4,6-9周\t张,李")[1])
    }
    @Test fun csvHandlesEncodingsQuotesAndNewlines() {
        val csv = "课程名称,教师\n\"数,学\",\"张\"\"老师\n李老师\""
        for (charset in listOf(Charsets.UTF_8, java.nio.charset.Charset.forName("GB18030"))) {
            assertEquals(listOf("数,学", "张\"老师\n李老师"), CsvReader.read(csv.toByteArray(charset).inputStream())[1])
        }
        val utf16 = byteArrayOf(0xff.toByte(), 0xfe.toByte()) + csv.toByteArray(Charsets.UTF_16LE)
        assertEquals("课程名称", CsvReader.read(utf16.inputStream()).first().first())
        assertTrue(runCatching { CsvReader.parse("a,\"broken") }.isFailure)
    }
    @Test fun disjointPeriodsStaySeparateAndAdjacentMerge() {
        assertEquals(listOf(1 to 2, 5 to 6), Parser.parseTimes("1-16周星期一1-2节,5-6节").map { it.startPeriod to it.endPeriod })
        assertEquals(listOf(1 to 2, 5 to 6), Parser.parseTimes("1-16周星期一1-2,5-6节").map { it.startPeriod to it.endPeriod })
        assertEquals(14, Parser.parseTime("星期五10-12节,13-14节").endPeriod)
    }
    @Test fun multipleDaysPreserveSeparateWeekRanges() {
        val result = Parser.parseTimes("1-8周星期一1-2节,9-16周星期三3-4节")
        assertEquals(listOf(1, 3), result.map { it.weekday })
        assertEquals(listOf("1-8", "9-16"), result.map { it.weeks })
        assertEquals(listOf(1, 3), Parser.parseTimes("1-16周周一、周三1-2节").map { it.weekday })
    }
    @Test fun importRetainsUnassignedCoursesAndUsesConfiguredTimes() {
        val grid = listOf(listOf("课程名称", "上课时间"), listOf("数学", ""), listOf("英语", "周二09:00-10:40"))
        val parsed = Parser.parse(grid, 0, Parser.ColMap(0, -1, 1, -1, -1), listOf("09:00", "09:55", "11:00"))
        assertEquals(2, parsed.size)
        assertEquals(0, parsed[0].startPeriod)
        assertEquals(2, parsed[1].endPeriod)
        assertTrue(runCatching { Parser.parseTime("周一999999999999-999999999999节") }.isFailure)
    }
    private fun course(id: String, start: Int, end: Int) = Course(id, id, "", "", "", 1, start, end, "")
    @Test fun overlappingCoursesNeverShareColumn() {
        val slots = layoutCourses(listOf(course("a", 1, 3), course("b", 2, 4), course("c", 4, 5), course("d", 7, 8)), 12)
        for (a in slots) for (b in slots) if (a != b && a.course.startPeriod <= b.course.endPeriod && b.course.startPeriod <= a.course.endPeriod) assertNotEquals(a.column, b.column)
        assertEquals(1, slots.last().columns)
        assertEquals(2, slots.first().columns)
    }
    @Test fun invalidCourseDoesNotHideValidNeighbour() {
        assertEquals(listOf("good"), layoutCourses(listOf(course("bad", 1, 99), course("good", 1, 2)), 12).map { it.course.id })
    }
}
