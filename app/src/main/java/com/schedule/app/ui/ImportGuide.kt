package com.schedule.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 发给 AI 的课表整理提示词（与项目根目录 导入整理Prompt.txt 保持一致）。 */
const val IMPORT_PROMPT = """你是一个课表数据整理器。我会给你一份课表（可能是 Excel、CSV、PDF、截图或网页上复制的文字），它可能来自任何学校的教务系统，格式千奇百怪。你的任务只有一个：把它整理成【标准课表 CSV】。除 CSV 外不要输出任何其他文字、解释或 Markdown 表格。

════════ 输出格式 ════════

第一行必须是且只能是这个表头：
课程名称,教学班号,上课时间,上课地点,上课教师

之后每一行代表"一门课的一次上课安排"。同一门课每周上多次（不同星期或不同节次），必须拆成多行，一行一个时间段。使用半角逗号分隔；单元格内容本身含逗号时，用英文双引号包住该单元格。

════════ 「上课时间」字段语法（必须严格逐字遵守）════════

三段连写、顺序固定：周次 → 星期 → 节次

1. 周次：数字或区间+「周」，如 1-4周、17-18周；不连续的用半角逗号：1-4,6-9周；单周在后面加(单)，双周加(双)：1-16周(单)。
2. 星期：一律写 星期一、星期二、…、星期日。禁止写 周一、礼拜一、Mon。
3. 节次：一律写「起-止节」，如 10-12节、3-4节；只上一节写 5节。
4. ⚠禁止：输出中不得出现「08:30-10:10」这类钟点！来源如果只有钟点，必须按下方换算表换算成节次后再输出；出现 HH:mm 即为错误。
5. 三段中缺哪段就省略哪段，剩下的保持 周次→星期→节次 的相对顺序：
   - 不知道周次：星期一10-12节
   - 不知道星期：1-16周10-12节
   - 没排时间地点的课（实习、见习等）：1-16周
   - 只知道星期：星期一

════════ 钟点 → 节次换算 ════════

若来源给的是钟点（如 08:00-09:40）而没有节次，按下表换算（若来源明确给出了本校的节次时间表，则优先按来源的）：
第1节 8:00｜第2节 8:55｜第3节 10:00｜第4节 10:55｜第5节 14:00｜第6节 14:55｜第7节 16:00｜第8节 16:50｜第9节 19:00｜第10节 19:50｜第11节 20:45｜第12节 21:40｜第13节 22:30
钟点落在某节开始时间 ±30 分钟内即算该节；相邻两节连上写成「起-止节」。

════════ 其他字段规则 ════════

- 课程名称：保留原始名称；去掉 ★☆◆■〇 等上课类型标记符号；名称中不要有换行。
- 教学班号：来源中的 教学班号/课程代码/课程编码，原样填入；没有就留空。
- 上课地点：来源中的 地点/教室/场地；没有或写"未排"就留空。
- 上课教师：多位教师用「、」分隔；没有就留空。
- 铁律：绝对不要编造或推测来源里没有的信息，不确定的字段一律留空。
- 若来源包含多个学期，只整理最近的一个学期。

════════ 输出前自检（逐条确认后再输出）════════

1. 第一行表头五个列名一字不差、顺序一致；
2. 每个上课时间段独占一行，没有把同一门课的两次上课挤在同一行；
3. 上课时间字段只包含 周次/星期/节次 三种成分，写法与上文完全一致，没有出现 HH:mm 钟点；
4. 单双周已标 (单)/(双)；
5. 没有输出 CSV 以外的任何文字。

════════ 示例 ════════

示例一（格子式课表中位于"星期三"列、第3-4节行的格子，内容"中国现当代文学★ 3-18周 示例楼四栋-14402 甲老师"）：
中国现当代文学,,3-18周星期三3-4节,示例楼四栋-14402,甲老师

示例二（来源只有钟点：星期一 第1-2节 08:00-09:40 高等数学 1-16周 教1-101 丁老师，班号 TEST01）：
高等数学,TEST01,1-16周星期一1-2节,教1-101,丁老师

示例三（来源"其他课程"行：教育见习★张某某(共16周)/1-16周/无）：
教育见习,,1-16周,,张某某

现在整理我发给你的课表："""

/** 导入引导页：告诉用户怎么借 AI 把任意学校的课表整理成标准格式。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportGuideScreen(onBack: () -> Unit, onGotoPaste: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = topBarTransparent(),
                title = { Text("导入引导") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Text("任何学校的课表都能导入", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text("每个学校的教务系统导出的格式都不一样，别担心——让 AI 帮你统一整理成标准格式，四步就搞定：", color = Color(0xFF6A7180), fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
            GuideStep("1", "点击下方按钮，复制这段「课表整理提示词」。")
            GuideStep("2", "把提示词和你的课表一起发给任意 AI 助手（DeepSeek、ChatGPT、Kimi 都可以），截图、PDF、Excel、网页复制的文字都行。")
            GuideStep("3", "AI 会回你一段统一格式的表格文字，把整段完整复制下来。")
            GuideStep("4", "回到 App 的导入课表页，点「粘贴文本导入」，粘贴后点解析，确认无误保存就行。")
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                clipboard.setText(AnnotatedString(IMPORT_PROMPT))
                copied = true
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (copied) "已复制，去发给 AI 吧" else "复制提示词")
            }
            Spacer(Modifier.height(14.dp))
            Text("提示词全文（也可长按手动选择复制）", color = Color(0xFF8A90A0), fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.92f))
                    .padding(14.dp)
            ) {
                SelectionContainer {
                    Text(IMPORT_PROMPT, fontSize = 12.sp, lineHeight = 17.sp, color = Color(0xFF3A4152))
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onGotoPaste, modifier = Modifier.fillMaxWidth()) {
                Text("已整理好，去粘贴导入 →")
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun GuideStep(no: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { Text(no, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 14.sp, lineHeight = 20.sp, color = Color(0xFF2A3140))
    }
    Spacer(Modifier.height(8.dp))
}
