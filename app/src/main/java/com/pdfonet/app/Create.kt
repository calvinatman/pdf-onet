package com.pdfonet.app

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ToolCard(title: String, sub: String, enabled: Boolean = true, onClick: () -> Unit = {}) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(CCard).border(1.dp, CLine, shape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(title, color = if (enabled) CTxt else CMuted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(sub, color = CMuted, fontSize = 13.sp)
    }
}

@Composable
fun TemplatesScreen(ed: EditorState, s: AppSettings, onBack: () -> Unit, goHome: () -> Unit) {
    val ctx = LocalContext.current
    var mine by remember { mutableStateOf(Templates.load(ctx)) }
    var act by remember { mutableStateOf<Tpl?>(null) }
    var nameDlg by remember { mutableStateOf<String?>(null) }
    var exportT by remember { mutableStateOf<Tpl?>(null) }
    BackHandler { onBack() }

    fun put(name: String, body: String) {
        val list = mine.filter { it.name != name } + Tpl(System.currentTimeMillis(), name, body)
        mine = list
        Templates.save(ctx, list)
    }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        val t = exportT
        if (uri != null && t != null) {
            try {
                ctx.contentResolver.openOutputStream(uri)!!.use { it.write(t.body.toByteArray()) }
                toast(ctx, "Exported")
            } catch (e: Exception) {
                toast(ctx, "Export failed")
            }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val r = readTextFrom(ctx, uri)
            if (r != null) put(r.first.substringBeforeLast('.').ifBlank { "Imported" }, r.second)
            else toast(ctx, "Could not read file")
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 8.dp)) {
        BackRow("Templates", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("BUILT-IN", color = CMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Templates.builtIn.forEach { t -> ToolCard(t.name, "Tap to use") { act = t } }
            Text("MY TEMPLATES", color = CMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (mine.isEmpty()) Text("No saved templates yet.", color = CMuted, fontSize = 14.sp)
            mine.forEach { t -> ToolCard(t.name, "Tap for options") { act = t } }
            GhostButton("Save current text as template") {
                if (ed.tfv.text.isBlank()) toast(ctx, "Editor is empty") else nameDlg = "save"
            }
            GhostButton("Import template file") { import.launch(arrayOf("*/*")) }
        }
    }

    act?.let { t ->
        val own = t.id >= 0
        AlertDialog(
            onDismissRequest = { act = null }, containerColor = CCard, titleContentColor = CTxt, textContentColor = CMuted,
            title = { Text(t.name) },
            text = { Text(if (own) "Use it, rename, export or delete." else "Built-in template.") },
            confirmButton = {
                Column {
                    TextButton(onClick = { ed.load(t.body); s.updatePro(true); act = null; goHome() }) {
                        Text("Use (open in editor)", color = CAccent)
                    }
                    if (own) {
                        TextButton(onClick = { nameDlg = "rename" }) { Text("Rename", color = CAccent) }
                        TextButton(onClick = { exportT = t; act = null; export.launch(t.name + ".md") }) { Text("Export", color = CAccent) }
                        TextButton(onClick = {
                            val list = mine.filter { it.id != t.id }
                            mine = list
                            Templates.save(ctx, list)
                            act = null
                        }) { Text("Delete", color = CAccent) }
                    }
                    TextButton(onClick = { act = null }) { Text("Cancel", color = CMuted) }
                }
            }
        )
    }
    when (nameDlg) {
        "save" -> TextInputDialog("Template name", "", "e.g. Weekly report", { if (it.isNotBlank()) put(it, ed.tfv.text) }, { nameDlg = null })
        "rename" -> {
            val t = act
            if (t != null) {
                TextInputDialog("Rename template", t.name, "Name", {
                    if (it.isNotBlank()) {
                        val list = mine.map { x -> if (x.id == t.id) x.copy(name = it) else x }
                        mine = list
                        Templates.save(ctx, list)
                    }
                    act = null
                }, { nameDlg = null })
            }
        }
        else -> {}
    }
}
