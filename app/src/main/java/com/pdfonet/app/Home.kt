package com.pdfonet.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Tool(
    val id: String, val title: String, val sub: String,
    val cat: String, val icon: String, val ready: Boolean
)

val ToolList = listOf(
    Tool("text", "Text to PDF", "Type, paste or import", "Create", "text", true),
    Tool("image", "Image to PDF", "Photos, one per page", "Create", "img", true),
    Tool("scan", "Scan to PDF", "Camera or gallery", "Create", "cam", true),
    Tool("templates", "Templates", "Resume, letter, report", "Create", "tpl", true),
    Tool("merge", "Merge PDFs", "Join many into one", "Combine", "merge", false),
    Tool("mix", "Alternate & Mix", "Interleave two PDFs", "Combine", "layers", false),
    Tool("split", "Split PDF", "Pages or ranges", "Combine", "split", false),
    Tool("splittext", "Split by Text", "New file at a keyword", "Combine", "split", false),
    Tool("splitbm", "Split by Bookmarks", "One file per chapter", "Combine", "split", false),
    Tool("splithalf", "Split in Half", "Two files, or slices", "Combine", "split", false),
    Tool("splitsize", "Split by Size", "Parts under a size", "Combine", "split", false),
    Tool("zip", "PDF to ZIP", "Bundle PDFs together", "Combine", "zip", false),
    Tool("organize", "Organize Pages", "Reorder, rotate, delete", "Arrange", "grid", false),
    Tool("rotate", "Rotate PDF", "Pages or whole file", "Arrange", "rotate", false),
    Tool("flip", "Flip PDF", "Mirror every page", "Arrange", "flip", false),
    Tool("nup", "Pages Per Sheet", "2, 4, 6, 9 or 16", "Arrange", "grid", false),
    Tool("crop", "Crop & Resize", "Trim margins", "Arrange", "crop", false),
    Tool("sign", "Fill & Sign", "Type or draw on forms", "Mark up", "pen", false),
    Tool("watermark", "Add Watermark", "Text like CONFIDENTIAL", "Mark up", "layers", false),
    Tool("numbers", "Page Numbers", "Footer, left or right", "Mark up", "hash", false),
    Tool("redact", "Redact PDF", "Black out names", "Mark up", "redact", false),
    Tool("details", "Edit Details", "Title, author, subject", "Mark up", "info", false),
    Tool("compress", "Compress PDF", "Make files smaller", "Protect", "press", false),
    Tool("ocr", "OCR Scanned PDF", "Make text searchable", "Protect", "ocr", false),
    Tool("setpw", "Set Password", "Lock with a password", "Protect", "lock", false),
    Tool("rmpw", "Remove Password", "Save an unlocked copy", "Protect", "lock", false),
    Tool("pdf2img", "PDF to Images", "Each page as JPG", "More", "img", true),
    Tool("extract", "Extract Text", "Copy text out", "More", "text", false),
    Tool("reverse", "Reverse Pages", "Last page first", "More", "rotate", false),
    Tool("delpages", "Delete Pages", "Remove chosen pages", "More", "trash", false)
)

val ToolCategories = listOf("Create", "Combine", "Arrange", "Mark up", "Protect", "More")

fun catTitle(c: String): String = when (c) {
    "Create" -> "CREATE"
    "Combine" -> "COMBINE & SPLIT"
    "Arrange" -> "ARRANGE PAGES"
    "Mark up" -> "FILL & MARK UP"
    "Protect" -> "PROTECT & SHRINK"
    else -> "MORE"
}

@Composable
fun HomeScreen(
    ed: EditorState, s: AppSettings,
    onEditor: () -> Unit, onScan: () -> Unit, onTemplates: () -> Unit,
    onResult: (List<PdfResult>) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("All tools") }
    var busy by remember { mutableStateOf(false) }

    val imgPick = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty() && !busy) {
            busy = true
            scope.launch {
                try {
                    val base = "PDF-ONET-IMG-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                    val res = withContext(Dispatchers.Default) {
                        PdfEngine.fromImages(ctx, uris, s.style, s.tree, base, "Original")
                    }
                    onResult(res)
                } catch (e: Exception) {
                    toast(ctx, "Failed: " + (e.message ?: "error"))
                }
                busy = false
            }
        }
    }
    val pdfPick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !busy) {
            busy = true
            scope.launch {
                try {
                    val base = "PDF-ONET-PAGE-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                    val n = withContext(Dispatchers.Default) { PdfEngine.pdfToImages(ctx, uri, s.tree, base) }
                    val where = if (s.tree == null) "Pictures/PDF ONET" else folderLabel(s.tree)
                    toast(ctx, "Saved $n images to $where")
                } catch (e: Exception) {
                    toast(ctx, "Failed: " + (e.message ?: "error"))
                }
                busy = false
            }
        }
    }

    fun run(t: Tool) {
        when (t.id) {
            "text" -> onEditor()
            "image" -> imgPick.launch("image/*")
            "scan" -> onScan()
            "templates" -> onTemplates()
            "pdf2img" -> pdfPick.launch(arrayOf("application/pdf"))
            else -> toast(ctx, t.title + " is coming in the next update")
        }
    }

    val filtered = ToolList.filter {
        (cat == "All tools" || it.cat == cat) &&
            (q.isBlank() || it.title.contains(q, true) || it.sub.contains(q, true))
    }
    val pinned = ToolList.filter { it.id in s.pins }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Title("PDF ONET", "${ToolList.size} tools. Everything runs on this phone.")
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = q, onValueChange = { q = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Merge, split, compress...") },
                leadingIcon = { SvgIcon("search", CMuted, 20.dp) },
                shape = RoundedCornerShape(16.dp), colors = fieldColors()
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) { HeroCard(ed, onEditor) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf("All tools") + ToolCategories + listOf()).forEach { c ->
                    FilterChipX(c, cat == c) { cat = c }
                }
            }
        }
        if (pinned.isNotEmpty() && q.isBlank() && cat == "All tools") {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionLabel("PINNED \u00B7 ${pinned.size}") }
            items(pinned, key = { "p-" + it.id }) { t ->
                ToolTile(t, true, { s.togglePin(t.id) }) { run(t) }
            }
        }
        ToolCategories.forEach { c ->
            val list = filtered.filter { it.cat == c }
            if (list.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { SectionLabel(catTitle(c) + " \u00B7 " + list.size) }
                items(list, key = { "c-" + it.id }) { t ->
                    ToolTile(t, t.id in s.pins, { s.togglePin(t.id) }) { run(t) }
                }
            }
        }
        if (filtered.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("No tools match your search.", color = CMuted, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun SectionLabel(t: String) {
    Text(
        t, color = CMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 2.dp, top = 6.dp)
    )
}

@Composable
fun HeroCard(ed: EditorState, onOpen: () -> Unit) {
    val words = remember(ed.tfv.text) {
        val t = ed.tfv.text.trim()
        if (t.isEmpty()) 0 else t.split(Regex("\\s+")).size
    }
    val third = lerp(CAccent, Color.Black, 0.25f)
    val soft = Color(0xFFF5D9CD)
    val date = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date()) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF241512), Color(0xFF5A2314), third)))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(date, color = soft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text("Text to PDF", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            if (words > 0) "Continue your draft ($words words)"
            else "Type, paste or import. Pro mode adds Markdown, cover page and more.",
            color = soft, fontSize = 14.sp
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (words > 0) "Draft saved on this phone" else "Works offline", color = soft, fontSize = 12.sp)
            Box(
                Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFFFDEBE3))
                    .clickable(onClick = onOpen).padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (words > 0) "Continue \u2192" else "Open editor \u2192",
                    color = Color(0xFF1A1A1A), fontWeight = FontWeight.Bold, fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun ToolTile(t: Tool, pinned: Boolean, onPin: () -> Unit, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier.fillMaxWidth().heightIn(min = 108.dp).clip(shape).background(CCard)
            .border(1.dp, CLine, shape).clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(CLine),
                contentAlignment = Alignment.Center
            ) { SvgIcon(t.icon, if (t.ready) CAccent else CMuted, 22.dp) }
            Text(t.title, color = CTxt, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
            Text(if (t.ready) t.sub else "Coming soon", color = CMuted, fontSize = 12.sp, lineHeight = 15.sp)
        }
        Box(
            Modifier.align(Alignment.TopEnd).size(44.dp).clickable(onClick = onPin),
            contentAlignment = Alignment.Center
        ) { SvgIcon("star", if (pinned) CAccent else CMuted, 18.dp, filled = pinned) }
    }
}
