package com.pdfonet.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class Tab { Home, Create, History, Settings }

fun fmtSize(b: Long): String = when {
    b < 1024 -> "$b B"
    b < 1024 * 1024 -> "${b / 1024} KB"
    else -> String.format(Locale.US, "%.1f MB", b / 1048576.0)
}

fun toast(c: Context, m: String) = Toast.makeText(c, m, Toast.LENGTH_SHORT).show()

fun openPdf(c: Context, uri: String) {
    val i = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), "application/pdf")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try { c.startActivity(i) } catch (e: Exception) { toast(c, "No PDF viewer found") }
}

fun sharePdf(c: Context, uri: String) {
    val i = Intent(Intent.ACTION_SEND).setType("application/pdf")
        .putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try { c.startActivity(Intent.createChooser(i, "Share PDF")) } catch (e: Exception) { toast(c, "Cannot share") }
}

@Composable
fun PdfOnetApp() {
    val ctx = LocalContext.current
    var tab by remember { mutableStateOf(Tab.Home) }
    var result by remember { mutableStateOf<HistoryItem?>(null) }
    var legal by remember { mutableStateOf<Pair<String, String>?>(null) }
    var history by remember { mutableStateOf(Store.load(ctx)) }

    fun update(list: List<HistoryItem>) {
        history = list
        Store.save(ctx, list)
    }

    BackHandler(enabled = legal != null || result != null || tab != Tab.Home) {
        when {
            legal != null -> legal = null
            result != null -> result = null
            else -> tab = Tab.Home
        }
    }

    Box(Modifier.fillMaxSize().background(CBg).systemBarsPadding().imePadding()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                val l = legal
                val r = result
                when {
                    l != null -> LegalScreen(l.first, l.second) { legal = null }
                    r != null -> ResultScreen(r) { result = null; tab = Tab.Home }
                    tab == Tab.Home -> HomeScreen { item ->
                        update(listOf(item) + history)
                        result = item
                    }
                    tab == Tab.Create -> CreateScreen()
                    tab == Tab.History -> HistoryScreen(history) { update(it) }
                    else -> SettingsScreen { t, b -> legal = t to b }
                }
            }
            if (legal == null) NavBar(tab) { result = null; tab = it }
        }
    }
}

@Composable
fun NavBar(tab: Tab, onTab: (Tab) -> Unit) {
    val shape = RoundedCornerShape(32.dp)
    Row(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp)
            .fillMaxWidth().height(64.dp)
            .clip(shape).background(CNav).border(1.dp, CLine, shape).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Tab.values().forEach { t ->
            val sel = t == tab
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (sel) CAccent else Color.Transparent)
                    .clickable { onTab(t) }
                    .semantics { contentDescription = t.name },
                contentAlignment = Alignment.Center
            ) {
                NavIcon(t, if (sel) Color(0xFF1A1A1A) else Color(0xFFD6D6D6))
            }
        }
    }
}

@Composable
fun NavIcon(t: Tab, color: Color) {
    Canvas(Modifier.size(26.dp)) {
        val s = size.width / 24f
        val st = Stroke(width = 2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (t) {
            Tab.Home -> listOf(4f to 4f, 14f to 4f, 4f to 14f, 14f to 14f).forEach { (x, y) ->
                drawRoundRect(color, Offset(x * s, y * s), Size(6 * s, 6 * s), CornerRadius(s), style = st)
            }
            Tab.Create -> {
                drawRoundRect(color, Offset(3 * s, 8 * s), Size(18 * s, 12 * s), CornerRadius(3 * s), style = st)
                drawCircle(color, 3 * s, Offset(12 * s, 14 * s), style = st)
            }
            Tab.History -> {
                val p = Path().apply {
                    moveTo(3 * s, 6 * s); lineTo(9 * s, 6 * s); lineTo(11 * s, 8 * s)
                    lineTo(21 * s, 8 * s); lineTo(21 * s, 19 * s); lineTo(3 * s, 19 * s); close()
                }
                drawPath(p, color, style = st)
            }
            Tab.Settings -> {
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
fun Chip(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier.heightIn(min = 44.dp).clip(shape).background(CCard).border(1.dp, CLine, shape)
            .clickable(onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = CTxt, fontSize = 14.sp) }
}

@Composable
fun PrimaryButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CAccent, contentColor = Color(0xFF121212),
            disabledContainerColor = Color(0xFF3A3A3A), disabledContentColor = CMuted
        )
    ) { Text(label, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun GhostButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, CLine),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CTxt)
    ) { Text(label, fontSize = 16.sp) }
}

@Composable
fun Title(t: String, sub: String? = null) {
    Column {
        Text(t, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = CTxt)
        if (sub != null) Text(sub, color = CMuted, fontSize = 14.sp)
    }
}

@Composable
fun HomeScreen(onDone: (HistoryItem) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("A4") }
    var land by remember { mutableStateOf(false) }
    var fs by remember { mutableStateOf(12) }
    var busy by remember { mutableStateOf(false) }
    val fonts = listOf(10, 12, 14, 16, 18)
    val keys = PdfEngine.sizes.keys.toList()

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                text = ctx.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                toast(ctx, "Could not read file")
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Title("PDF ONET", "Paste text, tap Convert.")
        OutlinedTextField(
            value = text, onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth().weight(1f),
            placeholder = { Text("Paste or type your text here...") },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CCard, unfocusedContainerColor = CCard,
                focusedBorderColor = CAccent, unfocusedBorderColor = CLine,
                focusedTextColor = CTxt, unfocusedTextColor = CTxt,
                cursorColor = CAccent, focusedPlaceholderColor = CMuted, unfocusedPlaceholderColor = CMuted
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(size) { size = keys[(keys.indexOf(size) + 1) % keys.size] }
            Chip(if (land) "Landscape" else "Portrait") { land = !land }
            Chip("Font $fs") { fs = fonts[(fonts.indexOf(fs) + 1) % fonts.size] }
        }
        PrimaryButton(if (busy) "Converting..." else "Convert to PDF", enabled = text.isNotBlank() && !busy) {
            busy = true
            scope.launch {
                try {
                    val name = "PDF-ONET-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".pdf"
                    val r = withContext(Dispatchers.Default) {
                        PdfEngine.create(ctx, text, name, PdfOptions(size, land, fs))
                    }
                    onDone(HistoryItem(System.currentTimeMillis(), r.name, r.uri.toString(), r.pages, r.size, System.currentTimeMillis()))
                    text = ""
                } catch (e: Exception) {
                    toast(ctx, "Failed: " + (e.message ?: "unknown error"))
                }
                busy = false
            }
        }
        GhostButton("Import file") { pick.launch(arrayOf("text/*")) }
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
fun ResultScreen(item: HistoryItem, onAnother: () -> Unit) {
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Title("PDF ready", "${item.pages} pages · ${fmtSize(item.size)} · saved to Downloads/PDF ONET")
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CCard)
                .border(1.dp, CLine, RoundedCornerShape(16.dp)).padding(20.dp)
        ) { Text(item.name, color = CTxt, fontSize = 16.sp) }
        PrimaryButton("Open") { openPdf(ctx, item.uri) }
        GhostButton("Share") { sharePdf(ctx, item.uri) }
        GhostButton("Convert another", onAnother)
    }
}

@Composable
fun CreateScreen() {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Title("Create", "Coming in the next phases")
        listOf("From file", "Image to PDF", "Merge PDFs", "Templates").forEach {
            Box(
                Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(RoundedCornerShape(14.dp)).background(CCard)
                    .border(1.dp, CLine, RoundedCornerShape(14.dp)).padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) { Text(it, color = CTxt, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(items: List<HistoryItem>, onChange: (List<HistoryItem>) -> Unit) {
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    var sort by remember { mutableStateOf("Newest") }
    var ask by remember { mutableStateOf<HistoryItem?>(null) }
    val sorts = listOf("Newest", "Oldest", "Name", "Size")
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    var shown = items.filter { it.name.contains(q, ignoreCase = true) }
    shown = when (filter) {
        "Today" -> shown.filter { it.time >= startOfToday }
        "Favorites" -> shown.filter { it.fav }
        else -> shown
    }
    shown = when (sort) {
        "Oldest" -> shown.sortedBy { it.time }
        "Name" -> shown.sortedBy { it.name.lowercase() }
        "Size" -> shown.sortedByDescending { it.size }
        else -> shown.sortedByDescending { it.time }
    }
    val df = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Title("History")
        OutlinedTextField(
            value = q, onValueChange = { q = it }, singleLine = true,
            modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search PDFs") },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CCard, unfocusedContainerColor = CCard,
                focusedBorderColor = CAccent, unfocusedBorderColor = CLine,
                focusedTextColor = CTxt, unfocusedTextColor = CTxt,
                cursorColor = CAccent, focusedPlaceholderColor = CMuted, unfocusedPlaceholderColor = CMuted
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Today", "Favorites").forEach { f ->
                Chip(if (filter == f) "● $f" else f) { filter = f }
            }
            Chip("Sort: $sort") { sort = sorts[(sorts.indexOf(sort) + 1) % sorts.size] }
        }
        if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No PDFs yet. Convert something first.", color = CMuted, fontSize = 15.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id }) { it ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CCard)
                            .border(1.dp, CLine, RoundedCornerShape(14.dp))
                            .combinedClickable(onClick = { openPdf(ctx, it.uri) }, onLongClick = { ask = it })
                            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(it.name, color = CTxt, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${it.pages} pages · ${fmtSize(it.size)} · ${df.format(Date(it.time))}",
                                color = CMuted, fontSize = 12.sp
                            )
                        }
                        Box(
                            Modifier.size(48.dp).clickable {
                                onChange(items.map { x -> if (x.id == it.id) x.copy(fav = !x.fav) else x })
                            },
                            contentAlignment = Alignment.Center
                        ) { Text(if (it.fav) "★" else "☆", color = CAccent, fontSize = 22.sp) }
                    }
                }
            }
        }
    }

    ask?.let { target ->
        AlertDialog(
            onDismissRequest = { ask = null },
            containerColor = CCard, titleContentColor = CTxt, textContentColor = CMuted,
            title = { Text("Delete PDF?") },
            text = { Text(target.name) },
            confirmButton = {
                Column {
                    TextButton(onClick = {
                        onChange(items.filter { it.id != target.id }); ask = null
                    }) { Text("Remove from list only", color = CAccent) }
                    TextButton(onClick = {
                        runCatching { ctx.contentResolver.delete(Uri.parse(target.uri), null, null) }
                        onChange(items.filter { it.id != target.id }); ask = null
                    }) { Text("Delete file too", color = CAccent) }
                    TextButton(onClick = { ask = null }) { Text("Cancel", color = CMuted) }
                }
            }
        )
    }
}

@Composable
fun SettingsRow(label: String, value: String = "", onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = CTxt, fontSize = 16.sp)
        Text(value, color = CMuted, fontSize = 14.sp)
    }
}

@Composable
fun Group(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, color = CMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CCard)) { content() }
    }
}

@Composable
fun SettingsScreen(onLegal: (String, String) -> Unit) {
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Title("Settings")
        Group("STORAGE") { SettingsRow("Save folder", "Downloads/PDF ONET") }
        Group("LEGAL") {
            SettingsRow("Privacy Policy", "›") { onLegal("Privacy Policy", LegalTexts.privacy) }
            SettingsRow("Terms of Use", "›") { onLegal("Terms of Use", LegalTexts.terms) }
            SettingsRow("Open-source licenses", "›") { onLegal("Open-source licenses", LegalTexts.licenses) }
            SettingsRow("About", "v1.0 ›") { onLegal("About", LegalTexts.about) }
        }
        Group("CONTACT") {
            SettingsRow("Contact us", "Telegram @blvcknetwork ›") {
                try {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/blvcknetwork")))
                } catch (e: Exception) { toast(ctx, "No app to open the link") }
            }
        }
    }
}

@Composable
fun LegalScreen(title: String, body: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 8.dp)) {
        Text(
            "‹  $title", color = CAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.heightIn(min = 48.dp).clickable(onClick = onBack).padding(vertical = 12.dp)
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(body, color = CTxt, fontSize = 15.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(24.dp))
        }
    }
}
