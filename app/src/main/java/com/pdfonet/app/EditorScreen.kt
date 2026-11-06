package com.pdfonet.app

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
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

fun pagesLabel(n: Int): String = if (n == 1) "1 page" else "$n pages"

class EditCtx(
    val tfv: TextFieldValue,
    val onTfv: (TextFieldValue) -> Unit,
    val total: Int,
    val pro: Boolean,
    val onPrev: () -> Unit,
    val onNext: () -> Unit,
    val onDone: () -> Unit
)

class MdTransformation(private val base: TextUnit, private val dim: Color) : VisualTransformation {
    private val head = Regex("^\\s*(#{1,3})\\s")
    private val bold = Regex("\\*\\*(.+?)\\*\\*")
    private val ital = Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")
    private val code = Regex("`(.+?)`")

    override fun filter(text: AnnotatedString): TransformedText {
        val src = text.text
        val b = AnnotatedString.Builder(src)
        var off = 0
        for (line in src.split('\n')) {
            val len = line.length
            val hm = head.find(line)
            if (hm != null) {
                val mult = when (hm.groupValues[1].length) { 1 -> 1.5f; 2 -> 1.25f; else -> 1.1f }
                b.addStyle(SpanStyle(fontSize = base * mult, fontWeight = FontWeight.Bold), off, off + len)
                b.addStyle(SpanStyle(color = dim), off, off + hm.value.length)
            }
            for (m in bold.findAll(line)) {
                val a = off + m.range.first
                val z = off + m.range.last + 1
                b.addStyle(SpanStyle(fontWeight = FontWeight.Bold), a, z)
                b.addStyle(SpanStyle(color = dim), a, a + 2)
                b.addStyle(SpanStyle(color = dim), z - 2, z)
            }
            for (m in ital.findAll(line)) {
                val a = off + m.range.first
                val z = off + m.range.last + 1
                b.addStyle(SpanStyle(fontStyle = FontStyle.Italic), a, z)
                b.addStyle(SpanStyle(color = dim), a, a + 1)
                b.addStyle(SpanStyle(color = dim), z - 1, z)
            }
            for (m in code.findAll(line)) {
                val a = off + m.range.first
                val z = off + m.range.last + 1
                b.addStyle(SpanStyle(fontFamily = FontFamily.Monospace), a, z)
            }
            off += len + 1
        }
        return TransformedText(b.toAnnotatedString(), OffsetMapping.Identity)
    }
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
fun PageCell(pg: Paginated, idx: Int, widthPx: Int, onTap: () -> Unit) {
    val bmp by produceState<ImageBitmap?>(null, pg, idx, widthPx) {
        value = withContext(Dispatchers.Default) {
            try { PdfEngine.pageBitmap(pg, idx, widthPx).asImageBitmap() } catch (e: Exception) { null }
        }
    }
    val shape = RoundedCornerShape(6.dp)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(pg.pw.toFloat() / pg.ph).clip(shape).background(Color.White)
                .clickable(onClick = onTap)
        ) {
            val b = bmp
            if (b != null) Image(bitmap = b, contentDescription = "Page ${idx + 1}", modifier = Modifier.fillMaxSize())
        }
        Text("Page ${idx + 1}", color = CMuted, fontSize = 12.sp)
    }
}

@Composable
fun PageEditCell(
    pg: Paginated, idx: Int, e: EditCtx,
    tall: Boolean = false, modifier: Modifier = Modifier
) {
    val st = pg.st
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val fr = remember { FocusRequester() }
    val paper = Color(Palette.paper(st.paperColor))
    val tc = Color(Palette.text(st.textColor))
    val ff = remember(st.font) { FontFamily(Fonts.typeface(ctx, st.font)) }
    val shape = RoundedCornerShape(6.dp)
    LaunchedEffect(idx) {
        delay(150)
        try { fr.requestFocus() } catch (ex: Exception) { }
    }
    val base = if (tall) modifier else modifier.fillMaxWidth().aspectRatio(pg.pw.toFloat() / pg.ph)
    BoxWithConstraints(base.clip(shape).background(paper).border(3.dp, CAccent, shape)) {
        val scale = maxWidth.value / pg.pw
        val fsDp = st.fontSize * scale
        val fs = with(density) { fsDp.dp.toSp() }
        val lh = with(density) { (fsDp * (st.lineSpacing.toFloatOrNull() ?: 1.2f)).dp.toSp() }
        val mDp = (pg.m * scale).dp
        val hDp = (pg.hh * scale).dp
        val ta = when (st.align) {
            "Center" -> TextAlign.Center
            "Right" -> TextAlign.End
            "Justify" -> TextAlign.Justify
            else -> TextAlign.Start
        }
        Box(
            Modifier.fillMaxSize()
                .padding(start = mDp, end = mDp, top = mDp + hDp, bottom = mDp)
                .verticalScroll(rememberScrollState())
        ) {
            BasicTextField(
                value = e.tfv, onValueChange = e.onTfv,
                modifier = Modifier.fillMaxWidth().focusRequester(fr),
                textStyle = TextStyle(color = tc, fontSize = fs, lineHeight = lh, fontFamily = ff, textAlign = ta),
                cursorBrush = SolidColor(CAccent),
                visualTransformation = if (e.pro) MdTransformation(fs, tc.copy(alpha = 0.4f)) else VisualTransformation.None
            )
        }
        Row(
            Modifier.align(Alignment.TopEnd).padding(6.dp)
                .background(Color(0xE6121212), RoundedCornerShape(22.dp)).padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Page ${idx + 1}/${e.total}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            IconBtnTint("chevL", "Previous page", Color.White, e.onPrev)
            IconBtnTint("chevR", "Next page", Color.White, e.onNext)
            IconBtnTint("check", "Done", CAccent, e.onDone)
        }
    }
}

@Composable
fun PagesArea(
    pg: Paginated?, cols: Int, active: Int?, ectx: EditCtx?,
    onTap: (Int) -> Unit, modifier: Modifier = Modifier
) {
    val gs = rememberLazyGridState()
    LaunchedEffect(active) {
        val a = active
        if (a != null) {
            try { gs.animateScrollToItem(a) } catch (e: Exception) { }
        }
    }
    if (pg == null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Write something to see the pages.", color = CMuted, fontSize = 14.sp)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(cols), modifier = modifier, state = gs,
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(count = pg.pages.size, key = { it }) { i ->
                if (active == i && ectx != null) PageEditCell(pg, i, ectx)
                else PageCell(pg, i, if (cols == 1) 900 else 520) { onTap(i) }
            }
        }
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
    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val hideKeyboard: () -> Unit = { focusManager.clearFocus(); keyboard?.hide() }
    var busy by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf(false) }
    var fsMode by remember { mutableStateOf(0) }
    var fsFocus by remember { mutableStateOf<Int?>(null) }
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
    fun closeEdit() { active = null; fsFocus = null }
    BackHandler(enabled = active != null) { closeEdit() }

    LaunchedEffect(ed.tfv.text, s.pro, st, showPages, active) {
        if (showPages && active == null) {
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

    fun selectPage(i: Int): Boolean {
        val p = pg ?: return false
        val page = p.pages.getOrNull(i) ?: return false
        val a = page.srcStart()
        val z = page.srcEnd()
        if (a < 0 || z < a) {
            toast(ctx, "This page is generated (cover or contents)")
            return false
        }
        val doc = ed.tfv.text
        rs = a.coerceAtMost(doc.length)
        re = z.coerceIn(rs, doc.length)
        pageTfv = TextFieldValue(doc.substring(rs, re))
        active = i
        if (fsFocus != null) fsFocus = i
        tick()
        return true
    }

    fun openPage(i: Int) {
        if (active == null) {
            selectPage(i)
        } else {
            active = null
            scope.launch {
                val p = try {
                    withContext(Dispatchers.Default) { PdfEngine.paginateText(ctx, ed.tfv.text, s.pro, st) }
                } catch (e: Exception) {
                    null
                }
                if (p != null && p.pages.isNotEmpty()) {
                    pg = p
                    selectPage(i.coerceIn(0, p.pages.size - 1))
                }
            }
        }
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
                val base = "PDio-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
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

    val onUndo = { ed.undo(); closeEdit() }
    val onRedo = { ed.redo(); closeEdit() }
    val onFind = { preview = false; if (full) fsMode = 2; closeEdit(); find = true }
    val words = remember(ed.tfv.text) {
        val t = ed.tfv.text.trim()
        if (t.isEmpty()) 0 else t.split(Regex("\\s+")).size
    }
    val total = pg?.pages?.size ?: 0
    val ectx: EditCtx? = if (active != null) EditCtx(
        pageTfv, { writePage(it, false) }, total, s.pro,
        { openPage((active ?: 0) - 1) }, { openPage((active ?: 0) + 1) }, { closeEdit() }
    ) else null

    if (!full) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!imeOpen) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "\u2039  Text to PDF", color = CAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).clickable(onClick = onBack).padding(vertical = 12.dp)
                    )
                    Text("Pro", color = CTxt, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = s.pro, onCheckedChange = { s.updatePro(it); closeEdit() }, colors = switchColors())
                    Spacer(Modifier.width(4.dp))
                    IconBtn("full", "Full screen") { onFull(true) }
                }
            }
            Row(
                Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FormatBar({ op -> act(op) }, { pickImg.launch("image/*") }, Modifier.weight(1f))
                if (imeOpen) {
                    IconGroup(onUndo, onRedo, onFind)
                    IconBtn("check", "Hide keyboard") { hideKeyboard() }
                }
            }
            if (!imeOpen) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChipX("Edit", !preview) { preview = false; closeEdit() }
                        FilterChipX("Preview", preview) { preview = true }
                    }
                    IconGroup(onUndo, onRedo, onFind)
                }
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
                        val a = active
                        val pgNow = pg
                        if (a != null && pgNow != null && ectx != null) {
                            PageEditCell(pgNow, a, ectx, tall = true, modifier = Modifier.fillMaxSize())
                        } else {
                            PagesArea(pg, 1, active, ectx, { openPage(it) }, Modifier.fillMaxSize())
                        }
                    }
                }
            }
            if (!imeOpen) {
                Text(
                    if (preview) pagesLabel(total) + " \u00B7 tap a page to edit it" else "$words words \u00B7 ${ed.tfv.text.length} chars",
                    color = CMuted, fontSize = 12.sp
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(st.pageSize) { dlg = "size" }
                    Chip(if (st.landscape) "Landscape" else "Portrait") { dlg = "orient" }
                    Chip(st.font) { dlg = "font" }
                    Chip("Size ${st.fontSize}") { dlg = "fsize" }
                    Chip("More \u203A") { onOptions() }
                    Chip("Clear") { ed.apply(TextFieldValue("")); closeEdit() }
                }
                PrimaryButton(if (busy) "Converting..." else "Convert to PDF", enabled = ed.tfv.text.isNotBlank() && !busy) { convert() }
            }
        }
    } else {
        val docTitle = ed.tfv.text.lineSequence().firstOrNull { it.isNotBlank() }
            ?.trimStart('#', ' ')?.take(28) ?: "Untitled"
        val modeLabel = when (fsMode) { 0 -> "Pages \u00B72 col"; 1 -> "Pages \u00B71 col"; else -> "Text" }
        val focus = fsFocus
        val pgNow = pg
        Column(Modifier.fillMaxSize()) {
            if (!imeOpen) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconBtn("exit", "Exit full screen") { closeEdit(); onFull(false) }
                    Column(Modifier.weight(1f)) {
                        Text(docTitle, color = CTxt, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("${pagesLabel(total)} \u00B7 ${st.pageSize} \u00B7 ${st.font} ${st.fontSize}", color = CMuted, fontSize = 11.sp, maxLines = 1)
                    }
                    Chip(modeLabel) { dlg = "fsmode" }
                    Box(
                        Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp)).background(CAccent)
                            .clickable { convert() }.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(if (busy) "..." else "Convert", color = COnAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FormatBar({ op -> act(op) }, { pickImg.launch("image/*") }, Modifier.weight(1f))
                IconGroup(onUndo, onRedo, onFind)
                if (imeOpen) IconBtn("check", "Hide keyboard") { hideKeyboard() }
            }
            Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (fsMode == 2) {
                    OutlinedTextField(
                        value = ed.tfv, onValueChange = { ed.onChange(it) },
                        modifier = Modifier.fillMaxSize(),
                        placeholder = { Text("Write here...") },
                        shape = RoundedCornerShape(16.dp), colors = fieldColors()
                    )
                } else if (focus != null && pgNow != null && ectx != null) {
                    PageEditCell(pgNow, focus, ectx, tall = true, modifier = Modifier.fillMaxSize())
                } else if (fsMode == 0) {
                    PagesArea(pg, 2, active, ectx, { i -> if (selectPage(i)) fsFocus = i }, Modifier.fillMaxSize())
                } else {
                    PagesArea(pg, 1, active, ectx, { openPage(it) }, Modifier.fillMaxSize())
                }
            }
        }
    }

    StyleDialogs(dlg, s) { dlg = null }
    if (dlg == "fsmode") {
        PickerDialog(
            "View", listOf("Pages (2 columns)", "Pages (1 column)", "Text"),
            when (fsMode) { 0 -> "Pages (2 columns)"; 1 -> "Pages (1 column)"; else -> "Text" },
            { fsMode = when (it) { "Pages (2 columns)" -> 0; "Pages (1 column)" -> 1; else -> 2 }; closeEdit() },
            { dlg = null }
        )
    }
    if (find) FindDialog(ed) { find = false }
}
