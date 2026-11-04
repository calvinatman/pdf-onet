package com.pdfonet.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

enum class NavTab { Home, Create, History, Settings }

@Composable
fun PdfOnetApp(s: AppSettings) {
    val ctx = LocalContext.current
    val ed = remember { EditorState(ctx) }
    var tab by remember { mutableStateOf(NavTab.Home) }
    var result by remember { mutableStateOf<List<HistoryItem>?>(null) }
    var legal by remember { mutableStateOf<Pair<String, String>?>(null) }
    var options by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(Store.load(ctx)) }

    fun update(list: List<HistoryItem>) {
        history = list
        Store.save(ctx, list)
    }

    fun finish(res: List<PdfResult>) {
        val now = System.currentTimeMillis()
        val items = res.mapIndexed { i, r -> HistoryItem(now + i, r.name, r.uri.toString(), r.pages, r.size, now) }
        update(items + history)
        result = items
        if (s.afterConvert == "Open PDF directly" && items.isNotEmpty()) openPdf(ctx, items[0].uri)
    }

    LaunchedEffect(Unit) {
        val days = s.autoClean
        if (days > 0) {
            val cutoff = System.currentTimeMillis() - days * 86400000L
            val old = history.filter { it.time < cutoff && !it.fav }
            if (old.isNotEmpty()) {
                withContext(Dispatchers.IO) { old.forEach { deleteDoc(ctx, it.uri) } }
                update(history.filter { it !in old })
            }
        }
    }
    LaunchedEffect(ed.tfv.text) {
        delay(700)
        withContext(Dispatchers.IO) { ed.saveDraft() }
    }

    BackHandler(enabled = legal != null || options || result != null || tab != NavTab.Home) {
        when {
            legal != null -> legal = null
            options -> options = false
            result != null -> result = null
            else -> tab = NavTab.Home
        }
    }

    Box(Modifier.fillMaxSize().background(CBg).systemBarsPadding().imePadding()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                val l = legal
                val r = result
                when {
                    l != null -> LegalScreen(l.first, l.second) { legal = null }
                    options -> OptionsScreen(s) { options = false }
                    r != null -> ResultScreen(r, s) { result = null; tab = NavTab.Home }
                    tab == NavTab.Home -> HomeScreen(ed, s, { options = true }) { finish(it) }
                    tab == NavTab.Create -> CreateScreen(ed, s, { tab = NavTab.Home }) { finish(it) }
                    tab == NavTab.History -> HistoryScreen(history) { update(it) }
                    else -> SettingsScreen(s, { t, b -> legal = t to b }) { update(emptyList()) }
                }
            }
            if (legal == null && !options) NavBar(tab) { result = null; tab = it }
        }
    }
}

@Composable
fun NavBar(tab: NavTab, onTab: (NavTab) -> Unit) {
    val shape = RoundedCornerShape(32.dp)
    Row(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp)
            .fillMaxWidth().height(64.dp)
            .clip(shape).background(CNav).border(1.dp, CLine, shape).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        NavTab.values().forEach { t ->
            val sel = t == tab
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (sel) CAccent else Color.Transparent)
                    .clickable { onTab(t) }
                    .semantics { contentDescription = t.name },
                contentAlignment = Alignment.Center
            ) {
                NavIcon(t, if (sel) Color(0xFF1A1A1A) else CTxt)
            }
        }
    }
}

@Composable
fun NavIcon(t: NavTab, color: Color) {
    Canvas(Modifier.size(26.dp)) {
        val s = size.width / 24f
        val st = Stroke(width = 2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (t) {
            NavTab.Home -> {
                val roof = Path().apply { moveTo(3 * s, 11 * s); lineTo(12 * s, 3 * s); lineTo(21 * s, 11 * s) }
                drawPath(roof, color, style = st)
                val body = Path().apply { moveTo(5 * s, 10 * s); lineTo(5 * s, 20 * s); lineTo(19 * s, 20 * s); lineTo(19 * s, 10 * s) }
                drawPath(body, color, style = st)
                val door = Path().apply { moveTo(10 * s, 20 * s); lineTo(10 * s, 14 * s); lineTo(14 * s, 14 * s); lineTo(14 * s, 20 * s) }
                drawPath(door, color, style = st)
            }
            NavTab.Create -> {
                drawCircle(color, 3.5f * s, Offset(16.5f * s, 7.5f * s), style = st)
                drawLine(color, Offset(14f * s, 10f * s), Offset(5f * s, 19f * s), strokeWidth = 3f * s, cap = StrokeCap.Round)
                drawLine(color, Offset(5f * s, 5f * s), Offset(10f * s, 10f * s), strokeWidth = 2f * s, cap = StrokeCap.Round)
                drawLine(color, Offset(13f * s, 13f * s), Offset(19f * s, 19f * s), strokeWidth = 3f * s, cap = StrokeCap.Round)
            }
            NavTab.History -> {
                val p = Path().apply {
                    moveTo(3 * s, 6 * s); lineTo(9 * s, 6 * s); lineTo(11 * s, 8 * s)
                    lineTo(21 * s, 8 * s); lineTo(21 * s, 19 * s); lineTo(3 * s, 19 * s); close()
                }
                drawPath(p, color, style = st)
            }
            NavTab.Settings -> {
                drawCircle(color, 3 * s, Offset(12 * s, 12 * s), style = st)
                drawCircle(
                    color, 8 * s, Offset(12 * s, 12 * s),
                    style = Stroke(width = 2f * s, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4 * s, 3 * s)))
                )
            }
        }
    }
}

@Composable
fun ResultScreen(items: List<HistoryItem>, s: AppSettings, onAnother: () -> Unit) {
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (items.size == 1) {
            Title("PDF ready", "${items[0].pages} pages \u00B7 ${fmtSize(items[0].size)}")
        } else {
            Title("${items.size} PDFs ready", "Split into ${items.size} files")
        }
        Text("Saved to: " + folderLabel(s.tree), color = CMuted, fontSize = 13.sp)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.id }) { it ->
                val shape = RoundedCornerShape(14.dp)
                Row(
                    Modifier.fillMaxWidth().clip(shape).background(CCard).border(1.dp, CLine, shape)
                        .clickable { openPdf(ctx, it.uri) }.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Thumb(it.uri, Modifier.width(42.dp).height(56.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(it.name, color = CTxt, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("${it.pages} pages \u00B7 ${fmtSize(it.size)}", color = CMuted, fontSize = 12.sp)
                    }
                    Text("Open", color = CAccent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                }
            }
        }
        if (items.size == 1) PrimaryButton("Open") { openPdf(ctx, items[0].uri) }
        GhostButton(if (items.size == 1) "Share" else "Share all") { sharePdfs(ctx, items.map { it.uri }) }
        GhostButton("Convert another", onAnother)
    }
}

@Composable
fun LegalScreen(title: String, body: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 8.dp)) {
        BackRow(title, onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(body, color = CTxt, fontSize = 15.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(24.dp))
        }
    }
}
