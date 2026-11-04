package com.pdfonet.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipInputStream

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

fun sharePdfs(c: Context, uris: List<String>) {
    if (uris.isEmpty()) return
    val i = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, Uri.parse(uris[0]))
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE)
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris.map { Uri.parse(it) }))
    }
    i.setType("application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try { c.startActivity(Intent.createChooser(i, "Share PDF")) } catch (e: Exception) { toast(c, "Cannot share") }
}

fun openLink(c: Context, url: String) {
    try { c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { toast(c, "No app to open the link") }
}

fun deleteDoc(c: Context, uri: String) {
    runCatching {
        val u = Uri.parse(uri)
        if (DocumentsContract.isDocumentUri(c, u)) DocumentsContract.deleteDocument(c.contentResolver, u)
        else c.contentResolver.delete(u, null, null)
    }
}

fun folderLabel(tree: String?): String {
    if (tree == null) return "Downloads/PDF ONET"
    return try {
        DocumentsContract.getTreeDocumentId(Uri.parse(tree)).replace("primary:", "").replace(":", "/").ifBlank { "Internal storage" }
    } catch (e: Exception) {
        "Custom folder"
    }
}

fun docxText(ins: InputStream): String {
    val z = ZipInputStream(ins)
    var xml = ""
    while (true) {
        val e = z.nextEntry ?: break
        if (e.name == "word/document.xml") {
            xml = z.readBytes().toString(Charsets.UTF_8)
            break
        }
    }
    val para = Regex("<w:p[ >].*?</w:p>", RegexOption.DOT_MATCHES_ALL)
    val tx = Regex("<w:t(?: [^>]*)?>(.*?)</w:t>", RegexOption.DOT_MATCHES_ALL)
    val sb = StringBuilder()
    for (p in para.findAll(xml)) {
        val t = tx.findAll(p.value).joinToString("") { it.groupValues[1] }
        sb.append(
            t.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&apos;", "'")
        ).append('\n')
    }
    return sb.toString().trim()
}

fun readTextFrom(ctx: Context, uri: Uri): Pair<String, String>? {
    return try {
        var name = ""
        ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0) name = c.getString(i) ?: ""
            }
        }
        val ins = ctx.contentResolver.openInputStream(uri) ?: return null
        val text = if (name.endsWith(".docx", true)) ins.use { docxText(it) } else ins.bufferedReader().use { it.readText() }
        Pair(name, text)
    } catch (e: Exception) {
        null
    }
}

fun saveEditorImage(ctx: Context, uri: Uri): String? {
    return try {
        val bmp = PdfEngine.decode(ctx, uri, 1600) ?: return null
        val dir = File(ctx.filesDir, "images").apply { mkdirs() }
        val name = "img" + System.currentTimeMillis() + ".jpg"
        File(dir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        name
    } catch (e: Exception) {
        null
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = CCard, unfocusedContainerColor = CCard,
    focusedBorderColor = CAccent, unfocusedBorderColor = CLine,
    focusedTextColor = CTxt, unfocusedTextColor = CTxt,
    cursorColor = CAccent, focusedPlaceholderColor = CMuted, unfocusedPlaceholderColor = CMuted
)

@Composable
fun switchColors(): SwitchColors = SwitchDefaults.colors(
    checkedTrackColor = CAccent, checkedThumbColor = Color.White,
    uncheckedTrackColor = CLine, uncheckedThumbColor = CMuted, uncheckedBorderColor = CLine
)

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
            containerColor = CAccent, contentColor = COnAccent,
            disabledContainerColor = CLine, disabledContentColor = CMuted
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
fun BackRow(title: String, onBack: () -> Unit) {
    Text(
        "\u2039  $title", color = CAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.heightIn(min = 48.dp).clickable(onClick = onBack).padding(vertical = 12.dp)
    )
}

@Composable
fun SettingsRow(label: String, value: String = "", onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = CTxt, fontSize = 16.sp)
        Text(value, color = CMuted, fontSize = 14.sp)
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = CTxt, fontSize = 16.sp)
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onChange, colors = switchColors())
    }
}

@Composable
fun Group(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title, color = CMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CCard)) { content() }
    }
}

@Composable
fun PickerDialog(title: String, options: List<String>, selected: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CCard, titleContentColor = CTxt, textContentColor = CTxt,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { o ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onPick(o); onDismiss() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (o == selected) "\u25C9" else "\u25CB", color = CAccent, fontSize = 18.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(o, color = CTxt, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = CMuted) } }
    )
}

@Composable
fun TextInputDialog(title: String, initial: String, hint: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var v by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CCard, titleContentColor = CTxt, textContentColor = CTxt,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = v, onValueChange = { v = it }, singleLine = true,
                placeholder = { Text(hint) }, colors = fieldColors()
            )
        },
        confirmButton = { TextButton(onClick = { onSave(v.trim()); onDismiss() }) { Text("Save", color = CAccent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = CMuted) } }
    )
}

@Composable
fun ConfirmDialog(title: String, msg: String, yes: String, onYes: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CCard, titleContentColor = CTxt, textContentColor = CMuted,
        title = { Text(title) }, text = { Text(msg) },
        confirmButton = { TextButton(onClick = { onYes(); onDismiss() }) { Text(yes, color = CAccent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = CMuted) } }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FontPickerDialog(selected: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var tick by remember { mutableStateOf(0) }
    val names = remember(tick) { Fonts.names(ctx) }
    val imp = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val n = Fonts.importFont(ctx, uri)
            if (n != null) { tick++; toast(ctx, "Added: $n") } else toast(ctx, "Not a valid .ttf / .otf font")
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CCard, titleContentColor = CTxt, textContentColor = CTxt,
        title = { Text("Font") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                names.forEach { n ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).combinedClickable(
                            onClick = { onPick(n); onDismiss() },
                            onLongClick = {
                                if (Fonts.isCustom(n)) { Fonts.remove(ctx, n); tick++; toast(ctx, "Font removed") }
                            }
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (n == selected) "\u25C9" else "\u25CB", color = CAccent, fontSize = 18.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(n, color = CTxt, fontSize = 17.sp, fontFamily = FontFamily(Fonts.typeface(ctx, n)))
                    }
                }
                Text(
                    "+ Import font (.ttf / .otf)", color = CAccent, fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .clickable { imp.launch(arrayOf("*/*")) }.padding(top = 14.dp)
                )
                Text("Long-press a custom font to remove it", color = CMuted, fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = CMuted) } }
    )
}

@Composable
fun StyleDialogs(key: String?, s: AppSettings, onClose: () -> Unit) {
    val st = s.style
    when (key) {
        "size" -> PickerDialog("Page size", PdfEngine.sizes.keys.toList(), st.pageSize, { s.saveStyle(st.copy(pageSize = it)) }, onClose)
        "orient" -> PickerDialog("Orientation", listOf("Portrait", "Landscape"), if (st.landscape) "Landscape" else "Portrait",
            { s.saveStyle(st.copy(landscape = it == "Landscape")) }, onClose)
        "font" -> FontPickerDialog(st.font, { s.saveStyle(st.copy(font = it)) }, onClose)
        "fsize" -> PickerDialog("Font size", listOf("8", "9", "10", "11", "12", "14", "16", "18", "20", "24"),
            st.fontSize.toString(), { s.saveStyle(st.copy(fontSize = it.toInt())) }, onClose)
        "margin" -> PickerDialog("Margins", listOf("Narrow", "Normal", "Wide"), st.margin, { s.saveStyle(st.copy(margin = it)) }, onClose)
        "spacing" -> PickerDialog("Line spacing", listOf("1.0", "1.2", "1.5", "2.0"), st.lineSpacing, { s.saveStyle(st.copy(lineSpacing = it)) }, onClose)
        "align" -> PickerDialog("Alignment", listOf("Left", "Center", "Right", "Justify"), st.align, { s.saveStyle(st.copy(align = it)) }, onClose)
        "tcolor" -> PickerDialog("Text colour", TextColors.keys.toList(), st.textColor, { s.saveStyle(st.copy(textColor = it)) }, onClose)
        "pcolor" -> PickerDialog("Paper colour", PaperColors.keys.toList(), st.paperColor, { s.saveStyle(st.copy(paperColor = it)) }, onClose)
        "pnum" -> PickerDialog("Page numbers", listOf("None", "1", "Page 1", "Page 1 of N"), st.pageNum, { s.saveStyle(st.copy(pageNum = it)) }, onClose)
        "split" -> PickerDialog("Split output", listOf("Off", "Every N pages", "By heading"), st.splitMode, { s.saveStyle(st.copy(splitMode = it)) }, onClose)
        "splitn" -> PickerDialog("Pages per file", listOf("2", "3", "5", "10", "20", "50"), st.splitN.toString(), { s.saveStyle(st.copy(splitN = it.toInt())) }, onClose)
        else -> {}
    }
}

@Composable
fun SaveFolderRow(s: AppSettings) {
    val ctx = LocalContext.current
    var ask by remember { mutableStateOf(false) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) { }
            s.setTree(uri.toString())
        }
    }
    SettingsRow("Save folder", folderLabel(s.tree) + " \u203A") { ask = true }
    if (ask) {
        PickerDialog(
            "Save folder", listOf("Choose folder...", "Use default (Downloads/PDF ONET)"), "",
            { if (it.startsWith("Choose")) pick.launch(null) else s.setTree(null) },
            { ask = false }
        )
    }
}

@Composable
fun Thumb(uri: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val bmp by produceState<ImageBitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            try {
                ctx.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { pfd ->
                    PdfRenderer(pfd).use { r ->
                        if (r.pageCount == 0) null else r.openPage(0).use { p ->
                            val b = Bitmap.createBitmap(96, (96f * p.height / p.width).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                            b.eraseColor(android.graphics.Color.WHITE)
                            p.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            b.asImageBitmap()
                        }
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    Box(modifier.clip(RoundedCornerShape(6.dp)).background(Color.White), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) Image(bitmap = b, contentDescription = null, modifier = Modifier.fillMaxSize())
    }
}
