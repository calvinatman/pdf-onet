package com.pdfonet.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OptionsScreen(s: AppSettings, onBack: () -> Unit) {
    val st = s.style
    var dlg by remember { mutableStateOf<String?>(null) }
    var inp by remember { mutableStateOf<String?>(null) }
    val arrow = " \u203A"

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 8.dp)) {
        BackRow("PDF options", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Group("LAYOUT") {
                SettingsRow("Page size", st.pageSize + arrow) { dlg = "size" }
                SettingsRow("Orientation", (if (st.landscape) "Landscape" else "Portrait") + arrow) { dlg = "orient" }
                SettingsRow("Margins", st.margin + arrow) { dlg = "margin" }
                SettingsRow("Line spacing", st.lineSpacing + arrow) { dlg = "spacing" }
                SettingsRow("Alignment", st.align + arrow) { dlg = "align" }
            }
            Group("TEXT") {
                SettingsRow("Font", st.font.take(18) + arrow) { dlg = "font" }
                SettingsRow("Font size", "${st.fontSize} pt$arrow") { dlg = "fsize" }
                SettingsRow("Text colour", st.textColor + arrow) { dlg = "tcolor" }
                SettingsRow("Paper colour", st.paperColor + arrow) { dlg = "pcolor" }
            }
            Group("EXTRAS") {
                SettingsRow("Page numbers", st.pageNum + arrow) { dlg = "pnum" }
                SettingsRow("Header text", st.header.ifBlank { "Off" }.take(16) + arrow) { inp = "header" }
                SettingsRow("Footer text", st.footer.ifBlank { "Off" }.take(16) + arrow) { inp = "footer" }
                SettingsRow("Watermark", st.watermark.ifBlank { "Off" }.take(16) + arrow) { inp = "wm" }
                SettingsRow("Cover title", st.coverTitle.ifBlank { "Off" }.take(16) + arrow) { inp = "ctitle" }
                SettingsRow("Cover author", st.coverAuthor.ifBlank { "-" }.take(16) + arrow) { inp = "cauthor" }
                SwitchRow("Table of contents (Pro mode)", st.toc) { s.saveStyle(st.copy(toc = it)) }
            }
            Group("OUTPUT") {
                SettingsRow("Split output", st.splitMode + arrow) { dlg = "split" }
                if (st.splitMode == "Every N pages") {
                    SettingsRow("Pages per file", st.splitN.toString() + arrow) { dlg = "splitn" }
                }
                SaveFolderRow(s)
            }
            GhostButton("Reset options") { s.saveStyle(PdfStyle()) }
        }
    }

    StyleDialogs(dlg, s) { dlg = null }
    when (inp) {
        "header" -> TextInputDialog("Header text", st.header, "Shown at the top of every page", { s.saveStyle(st.copy(header = it)) }, { inp = null })
        "footer" -> TextInputDialog("Footer text", st.footer, "Shown at the bottom of every page", { s.saveStyle(st.copy(footer = it)) }, { inp = null })
        "wm" -> TextInputDialog("Watermark", st.watermark, "e.g. CONFIDENTIAL", { s.saveStyle(st.copy(watermark = it)) }, { inp = null })
        "ctitle" -> TextInputDialog("Cover title", st.coverTitle, "Leave empty for no cover page", { s.saveStyle(st.copy(coverTitle = it)) }, { inp = null })
        "cauthor" -> TextInputDialog("Cover author", st.coverAuthor, "Your name", { s.saveStyle(st.copy(coverAuthor = it)) }, { inp = null })
        else -> {}
    }
}
