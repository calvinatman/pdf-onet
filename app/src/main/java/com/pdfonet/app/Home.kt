package com.pdfonet.app

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(ed: EditorState, s: AppSettings, onOptions: () -> Unit, onResult: (List<PdfResult>) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf(false) }
    var dlg by remember { mutableStateOf<String?>(null) }
    var find by remember { mutableStateOf(false) }
    val st = s.style

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val r = readTextFrom(ctx, uri)
            if (r != null) ed.load(r.second) else toast(ctx, "Could not read file")
        }
    }
    val pickImg = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val n = withContext(Dispatchers.IO) { saveEditorImage(ctx, uri) }
                if (n != null) ed.apply(insertBlock(ed.tfv, "![image](img:$n)")) else toast(ctx, "Could not add image")
            }
        }
    }
    val words = remember(ed.tfv.text) {
        val t = ed.tfv.text.trim()
        if (t.isEmpty()) 0 else t.split(Regex("\\s+")).size
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Title("PDF ONET", if (s.pro) "Pro mode" else "Paste text, tap Convert.")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pro", color = CTxt, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Switch(checked = s.pro, onCheckedChange = { s.setPro(it); preview = false }, colors = switchColors())
            }
        }
        if (s.pro) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip("B") { ed.apply(wrapSel(ed.tfv, "**", "**")) }
                Chip("I") { ed.apply(wrapSel(ed.tfv, "*", "*")) }
                Chip("H1") { ed.apply(prefixLine(ed.tfv, "# ")) }
                Chip("H2") { ed.apply(prefixLine(ed.tfv, "## ")) }
                Chip("H3") { ed.apply(prefixLine(ed.tfv, "### ")) }
                Chip("List") { ed.apply(prefixLine(ed.tfv, "- ")) }
                Chip("1.") { ed.apply(prefixLine(ed.tfv, "1. ")) }
                Chip("Quote") { ed.apply(prefixLine(ed.tfv, "> ")) }
                Chip("Code") { ed.apply(wrapSel(ed.tfv, "`", "`")) }
                Chip("Line") { ed.apply(insertBlock(ed.tfv, "---")) }
                Chip("Break") { ed.apply(insertBlock(ed.tfv, "[pagebreak]")) }
                Chip("Img") { pickImg.launch("image/*") }
                Chip("Undo") { ed.undo() }
                Chip("Redo") { ed.redo() }
                Chip("Find") { find = true }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(if (!preview) "\u25CF Edit" else "Edit") { preview = false }
                Chip(if (preview) "\u25CF Preview" else "Preview") { preview = true }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (s.pro && preview) {
                MdPreview(ed.tfv.text, st)
            } else {
                OutlinedTextField(
                    value = ed.tfv, onValueChange = { ed.onChange(it) },
                    modifier = Modifier.fillMaxSize(),
                    placeholder = { Text(if (s.pro) "Write in Markdown..." else "Paste or type your text here...") },
                    shape = RoundedCornerShape(16.dp), colors = fieldColors()
                )
            }
        }
        Text("$words words \u00B7 ${ed.tfv.text.length} chars", color = CMuted, fontSize = 12.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(st.pageSize) { dlg = "size" }
            Chip(if (st.landscape) "Landscape" else "Portrait") { dlg = "orient" }
            Chip(st.font) { dlg = "font" }
            Chip("Size ${st.fontSize}") { dlg = "fsize" }
            Chip("More \u203A") { onOptions() }
            Chip("Clear") { ed.apply(TextFieldValue("")) }
        }
        PrimaryButton(if (busy) "Converting..." else "Convert to PDF", enabled = ed.tfv.text.isNotBlank() && !busy) {
            busy = true
            scope.launch {
                try {
                    val base = "PDF-ONET-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                    val res = withContext(Dispatchers.Default) {
                        PdfEngine.create(ctx, ed.tfv.text, s.pro, s.style, s.tree, base)
                    }
                    onResult(res)
                } catch (e: Exception) {
                    toast(ctx, "Failed: " + (e.message ?: "error"))
                }
                busy = false
            }
        }
        if (!s.pro) GhostButton("Import file") { pickFile.launch(arrayOf("*/*")) }
        Spacer(Modifier.height(2.dp))
    }

    StyleDialogs(dlg, s) { dlg = null }
    if (find) FindDialog(ed) { find = false }
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
fun MdPreview(text: String, st: PdfStyle) {
    val ctx = LocalContext.current
    val ff = remember(st.font) { FontFamily(Fonts.typeface(ctx, st.font)) }
    val lines = remember(text) { Md.parse(text) }
    val paper = Color(Palette.paper(st.paperColor))
    val tc = Color(Palette.text(st.textColor))
    LazyColumn(Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(paper).padding(16.dp)) {
        items(lines) { ml -> PreviewLine(ml, tc, ff, st.fontSize) }
    }
}

fun annotated(prefix: String, segs: List<Seg>): AnnotatedString = buildAnnotatedString {
    append(prefix)
    for (g in segs) {
        val sp = SpanStyle(
            fontWeight = if (g.b) FontWeight.Bold else null,
            fontStyle = if (g.i) FontStyle.Italic else null,
            fontFamily = if (g.c) FontFamily.Monospace else null,
            background = if (g.c) Color(0x22808080) else Color.Unspecified
        )
        withStyle(sp) { append(g.t) }
    }
}

@Composable
fun PreviewLine(ml: ML, tc: Color, ff: FontFamily, fs: Int) {
    val ctx = LocalContext.current
    val top = if (ml.gap) (fs * 0.6f).dp else 0.dp
    val m = Modifier.padding(top = top, bottom = 4.dp)
    when (ml.kind) {
        1 -> {
            val mult = when (ml.level) { 1 -> 1.9f; 2 -> 1.55f; else -> 1.25f }
            Text(annotated("", ml.segs), color = tc, fontFamily = ff, fontWeight = FontWeight.Bold, fontSize = (fs * mult).sp, modifier = Modifier.padding(top = top + 6.dp, bottom = 4.dp))
        }
        2 -> Text(annotated("\u2022  ", ml.segs), color = tc, fontFamily = ff, fontSize = fs.sp, modifier = Modifier.padding(start = (12 + ml.indent * 5).dp, top = top, bottom = 2.dp))
        3 -> Text(annotated(ml.prefix, ml.segs), color = tc, fontFamily = ff, fontSize = fs.sp, modifier = Modifier.padding(start = (12 + ml.indent * 5).dp, top = top, bottom = 2.dp))
        4 -> Text(annotated("\u258C ", ml.segs), color = tc, fontFamily = ff, fontStyle = FontStyle.Italic, fontSize = fs.sp, modifier = m)
        5 -> Text(ml.text, color = tc, fontFamily = FontFamily.Monospace, fontSize = (fs * 0.9f).sp, modifier = Modifier.fillMaxWidth().background(Color(0x1A808080)).padding(6.dp))
        6 -> Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).height(1.dp).background(tc.copy(alpha = 0.4f)))
        7 -> Text("\u2014 page break \u2014", color = Color(0xFF888888), fontSize = 12.sp, modifier = m)
        8 -> {
            val bmp by produceState<ImageBitmap?>(null, ml.text) {
                value = withContext(Dispatchers.IO) {
                    try {
                        val o = BitmapFactory.Options().apply { inSampleSize = 3 }
                        BitmapFactory.decodeFile(File(ctx.filesDir, "images/" + ml.text).path, o)?.asImageBitmap()
                    } catch (e: Exception) { null }
                }
            }
            val b = bmp
            if (b != null) Image(bitmap = b, contentDescription = null, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
            else Text("[image]", color = Color(0xFF888888), fontSize = 12.sp)
        }
        else -> Text(annotated("", ml.segs), color = tc, fontFamily = ff, fontSize = fs.sp, modifier = m)
    }
}
