package com.pdfonet.app

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun EditorState.replaceRange(s: Int, e: Int, t: String, push: Boolean) {
    val doc = tfv.text
    val a = s.coerceIn(0, doc.length)
    val b = e.coerceIn(a, doc.length)
    val nv = TextFieldValue(doc.substring(0, a) + t + doc.substring(b), TextRange(a + t.length))
    if (push) apply(nv) else onChange(nv)
}

@Composable
fun FormatBar(act: ((TextFieldValue) -> TextFieldValue) -> Unit, onImg: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Chip("B") { act { wrapSel(it, "**", "**") } }
        Chip("I") { act { wrapSel(it, "*", "*") } }
        Chip("H1") { act { prefixLine(it, "# ") } }
        Chip("H2") { act { prefixLine(it, "## ") } }
        Chip("H3") { act { prefixLine(it, "### ") } }
        Chip("List") { act { prefixLine(it, "- ") } }
        Chip("1.") { act { prefixLine(it, "1. ") } }
        Chip("Quote") { act { prefixLine(it, "> ") } }
        Chip("Code") { act { wrapSel(it, "`", "`") } }
        Chip("Line") { act { insertBlock(it, "---") } }
        Chip("Break") { act { insertBlock(it, "[pagebreak]") } }
        Chip("Img") { onImg() }
    }
}

@Composable
fun PageCell(pg: Paginated, idx: Int, widthPx: Int, active: Boolean, onTap: () -> Unit) {
    val bmp by produceState<ImageBitmap?>(null, pg, idx, widthPx) {
        value = withContext(Dispatchers.Default) {
            try { PdfEngine.pageBitmap(pg, idx, widthPx).asImageBitmap() } catch (e: Exception) { null }
        }
    }
    val shape = RoundedCornerShape(6.dp)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(pg.pw.toFloat() / pg.ph).clip(shape).background(Color.White)
                .then(if (active) Modifier.border(3.dp, CAccent, shape) else Modifier)
                .clickable(onClick = onTap)
        ) {
            val b = bmp
            if (b != null) Image(bitmap = b, contentDescription = "Page ${idx + 1}", modifier = Modifier.fillMaxSize())
            if (active) {
                Text(
                    "Editing", color = COnAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                        .background(CAccent, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        Text("Page ${idx + 1}", color = CMuted, fontSize = 12.sp)
    }
}

@Composable
fun PagesArea(pg: Paginated?, cols: Int, active: Int?, onTap: (Int) -> Unit, modifier: Modifier = Modifier) {
    if (pg == null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Write something to see the pages.", color = CMuted, fontSize = 14.sp)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(cols), modifier = modifier,
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(count = pg.pages.size, key = { it }) { i ->
                PageCell(pg, i, if (cols == 1) 900 else 520, active == i) { onTap(i) }
            }
        }
    }
}

@Composable
fun PagePanel(
    n: Int, total: Int, tfv: TextFieldValue, onChange: (TextFieldValue) -> Unit,
    onPrev: () -> Unit, onNext: () -> Unit, onDone: () -> Unit
) {
    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(CCard).border(2.dp, CAccent, shape).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Editing page $n of $total", color = CTxt, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip("\u2039") { onPrev() }
                Chip("\u203A") { onNext() }
                Chip("Done") { onDone() }
            }
        }
        OutlinedTextField(
            value = tfv, onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(12.dp), colors = fieldColors()
        )
    }
}

@Composable
fun FindDialog(ed: EditorState, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var f by remember { mutableStateOf("") }
    var r by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onClose, containerColor = CCard, titleContentColor = CTxt, textContentColor = CTxt,
        title = { Text("Find & replace") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = f, onValueChange = { f = it }, singleLine = true, placeholder = { Text("Find") }, colors = fieldColors())
                OutlinedTextField(value = r, onValueChange = { r = it }, singleLine = true, placeholder = { Text("Replace with") }, colors = fieldColors())
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = {
                    val t = ed.tfv.text
                    if (f.isNotEmpty()) {
                        var idx = t.indexOf(f, ed.tfv.selection.max, ignoreCase = true)
                        if (idx < 0) idx = t.indexOf(f, 0, ignoreCase = true)
                        if (idx >= 0) ed.tfv = ed.tfv.copy(selection = TextRange(idx, idx + f.length)) else toast(ctx, "Not found")
                    }
                }) { Text("Find next", color = CAccent) }
                TextButton(onClick = {
                    if (f.isNotEmpty()) ed.apply(TextFieldValue(ed.tfv.text.replace(f, r, ignoreCase = true)))
                }) { Text("Replace all", color = CAccent) }
            }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Close", color = CMuted) } }
    )
}

@Composable
fun EditorScreen(
    ed: EditorState, s: AppSettings, full: Boolean, onFull: (Boolean) -> Unit,
    onBack: () -> Unit, onOptions: () -> Unit, onResult: (List<PdfResult>) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val tick = rememberHaptic(s)
    val dur = if (s.anim) 240 else 0
    var busy by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf(false) }
    var fsMode by remember { mutableStateOf(0) }
    var dlg by remember { mutableStateOf<String?>(null) }
    var find by remember { mutableStateOf(false) }
    var pg by remember { mutableStateOf<Paginated?>(null) }
    var active by remember { mutableStateOf<Int?>(null) }
    var pageTfv by remember { mutableStateOf(TextFieldValue("")) }
    var rs by remember { mutableStateOf(0) }
    var re by remember { mutableStateOf(0) }
    val st = s.style
    val showPages = if (full) fsMode != 2 else preview

    BackHandler(enabled = full) { onFull(false) }

    LaunchedEffect(ed.tfv.text, s.pro, st, showPages) {
        if (showPages) {
            val text = ed.tfv.text
            if (text.isBlank()) {
                pg = null
            } else {
                delay(350)
                pg = try {
                    withContext(Dispatchers.Default) { PdfEngine.paginateText(ctx, text, s.pro, st) }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    fun selectPage(i: Int) {
        val p = pg ?: return
        val page = p.pages.getOrNull(i) ?: return
        val a = page.srcStart()
        val b = page.srcEnd()
        if (a < 0 || b < a) {
            toast(ctx, "This page is generated (cover or contents)")
            return
        }
        val doc = ed.tfv.text
        rs = a.coerceAtMost(doc.length)
        re = b.coerceIn(rs, doc.length)
        pageTfv = TextFieldValue(doc.substring(rs, re))
        active = i
        tick()
    }

    fun writePage(n: TextFieldValue, push: Boolean) {
        pageTfv = n
        ed.replaceRange(rs, re, n.text, push)
        re = rs + n.text.length
    }

    fun act(op: (TextFieldValue) -> TextFieldValue) {
        if (active != null && showPages) writePage(op(pageTfv), true) else ed.apply(op(ed.tfv))
    }

    val pickImg = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val n = withContext(Dispatchers.IO) { saveEditorImage(ctx, uri) }
                if (n != null) act { insertBlock(it, "![image](img:$n)") } else toast(ctx, "Could not add image")
            }
        }
    }

    fun convert() {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val base = "PDF-ONET-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                val res = withContext(Dispatchers.Default) {
                    PdfEngine.create(ctx, ed.tfv.text, s.pro, s.style, s.tree, base)
                }
                tick()
                onResult(res)
            } catch (e: Exception) {
                toast(ctx, "Failed: " + (e.message ?: "error"))
            }
            busy = false
        }
    }

    val onUndo = { ed.undo(); active = null }
    val onRedo = { ed.redo(); active = null }
    val onFind = { preview = false; if (full) fsMode = 2; find = true }
    val goPrev = { selectPage((active ?: 0) - 1) }
    val goNext = { selectPage((active ?: 0) + 1) }
    val words = remember(ed.tfv.text) {
        val t = ed.tfv.text.trim()
        if (t.isEmpty()) 0 else t.split(Regex("\\s+")).size
    }
    val total = pg?.pages?.size ?: 0

    @Composable
    fun Panel() {
        AnimatedVisibility(
            visible = active != null && showPages,
            enter = fadeIn(tween(dur)) + expandVertically(tween(dur)),
            exit = fadeOut(tween(dur)) + shrinkVertically(tween(dur))
        ) {
            PagePanel((active ?: 0) + 1, total, pageTfv, { writePage(it, false) }, goPrev, goNext) { active = null }
        }
    }

    if (!full) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "\u2039  Text to PDF", color = CAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).clickable(onClick = onBack).padding(vertical = 12.dp)
                )
                Text("Pro", color = CTxt, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Switch(checked = s.pro, onCheckedChange = { s.updatePro(it); active = null }, colors = switchColors())
                Spacer(Modifier.width(4.dp))
                IconBtn("full", "Full screen") { onFull(true) }
            }
            FormatBar({ op -> act(op) }, { pickImg.launch("image/*") })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChipX("Edit", !preview) { preview = false; active = null }
                    FilterChipX("Preview", preview) { preview = true }
                }
                IconGroup(onUndo, onRedo, onFind)
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                AnimatedContent(
                    targetState = preview,
                    transitionSpec = { fadeIn(tween(dur)) togetherWith fadeOut(tween(dur)) },
                    label = "mode"
                ) { p ->
                    if (!p) {
                        OutlinedTextField(
                            value = ed.tfv, onValueChange = { ed.onChange(it) },
                            modifier = Modifier.fillMaxSize(),
                            placeholder = { Text(if (s.pro) "Write in Markdown..." else "Paste or type your text here...") },
                            shape = RoundedCornerShape(16.dp), colors = fieldColors()
                        )
                    } else {
                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PagesArea(pg, 1, active, { selectPage(it) }, Modifier.weight(1f).fillMaxWidth())
                            Panel()
                        }
                    }
                }
            }
            Text(
                if (preview) "$total pages \u00B7 tap a page to edit it" else "$words words \u00B7 ${ed.tfv.text.length} chars",
                color = CMuted, fontSize = 12.sp
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(st.pageSize) { dlg = "size" }
                Chip(if (st.landscape) "Landscape" else "Portrait") { dlg = "orient" }
                Chip(st.font) { dlg = "font" }
                Chip("Size ${st.fontSize}") { dlg = "fsize" }
                Chip("More \u203A") { onOptions() }
                Chip("Clear") { ed.apply(TextFieldValue("")); active = null }
            }
            PrimaryButton(if (busy) "Converting..." else "Convert to PDF", enabled = ed.tfv.text.isNotBlank() && !busy) { convert() }
        }
    } else {
        val docTitle = ed.tfv.text.lineSequence().firstOrNull { it.isNotBlank() }
            ?.trimStart('#', ' ')?.take(28) ?: "Untitled"
        val modeLabel = when (fsMode) { 0 -> "Pages \u00B72 col"; 1 -> "Pages \u00B71 col"; else -> "Text" }
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconBtn("exit", "Exit full screen") { onFull(false) }
                Column(Modifier.weight(1f)) {
                    Text(docTitle, color = CTxt, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("$total pages \u00B7 ${st.pageSize} \u00B7 ${st.font} ${st.fontSize}", color = CMuted, fontSize = 11.sp, maxLines = 1)
                }
                Chip(modeLabel) { dlg = "fsmode" }
                Box(
                    Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp)).background(CAccent)
                        .clickable { convert() }.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) { Text(if (busy) "..." else "Convert", color = COnAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FormatBar({ op -> act(op) }, { pickImg.launch("image/*") }, Modifier.weight(1f))
                IconGroup(onUndo, onRedo, onFind)
            }
            Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (fsMode == 2) {
                    OutlinedTextField(
                        value = ed.tfv, onValueChange = { ed.onChange(it) },
                        modifier = Modifier.fillMaxSize(),
                        placeholder = { Text("Write here...") },
                        shape = RoundedCornerShape(16.dp), colors = fieldColors()
                    )
                } else {
                    PagesArea(pg, if (fsMode == 0) 2 else 1, active, { selectPage(it) }, Modifier.fillMaxSize())
                }
            }
            if (fsMode != 2) {
                if (active == null) {
                    Text(
                        "Tap a page to edit it", color = CMuted, fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, bottom = 10.dp)
                    )
                } else {
                    Panel()
                }
            }
        }
    }

    StyleDialogs(dlg, s) { dlg = null }
    if (dlg == "fsmode") {
        PickerDialog(
            "View", listOf("Pages (2 columns)", "Pages (1 column)", "Text"),
            when (fsMode) { 0 -> "Pages (2 columns)"; 1 -> "Pages (1 column)"; else -> "Text" },
            { fsMode = when (it) { "Pages (2 columns)" -> 0; "Pages (1 column)" -> 1; else -> 2 }; active = null },
            { dlg = null }
        )
    }
    if (find) FindDialog(ed) { find = false }
}
