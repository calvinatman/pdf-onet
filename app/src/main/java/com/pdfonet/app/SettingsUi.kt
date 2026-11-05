package com.pdfonet.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(s: AppSettings, onLegal: (String, String) -> Unit, onClearHistory: () -> Unit) {
    val ctx = LocalContext.current
    val st = s.style
    var dlg by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf<String?>(null) }
    val arrow = " \u203A"

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Title("Settings")
        Group("APPEARANCE") {
            SettingsRow("Theme", s.theme + arrow) { dlg = "theme" }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Accent", color = CTxt, fontSize = 16.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Accents.forEach { (n, v) ->
                        val on = s.accent == n
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(Color(v))
                                .border(if (on) 3.dp else 1.dp, if (on) CTxt else CLine, CircleShape)
                                .clickable { s.updateAccent(n) }
                                .semantics { contentDescription = n + " accent" }
                        )
                    }
                }
                Text(
                    "Your selected accent stays unchanged when you switch themes.",
                    color = CMuted, fontSize = 12.sp
                )
            }
        }
        Group("MOTION & FEEL") {
            SwitchRow("Animations", s.anim) { s.updateAnim(it) }
            SwitchRow("Haptic feedback", s.haptic) { s.updateHaptic(it) }
        }
        Group("PDF DEFAULTS") {
            SettingsRow("Page size", st.pageSize + arrow) { dlg = "size" }
            SettingsRow("Orientation", (if (st.landscape) "Landscape" else "Portrait") + arrow) { dlg = "orient" }
            SettingsRow("Font", st.font.take(18) + arrow) { dlg = "font" }
            SettingsRow("Font size", "${st.fontSize} pt$arrow") { dlg = "fsize" }
            SettingsRow("Margins", st.margin + arrow) { dlg = "margin" }
        }
        Group("STORAGE") { SaveFolderRow(s) }
        Group("BEHAVIOR") {
            SettingsRow("After convert", s.afterConvert + arrow) { dlg = "after" }
            SettingsRow("History auto-clean", (if (s.autoClean == 0) "Off" else "${s.autoClean} days") + arrow) { dlg = "clean" }
        }
        Group("DATA") {
            SettingsRow("Clear history", "List only$arrow") { confirm = "history" }
            SettingsRow("Reset all settings", arrow) { confirm = "reset" }
        }
        Group("LEGAL") {
            SettingsRow("Privacy Policy", "\u203A") { onLegal("Privacy Policy", LegalTexts.privacy) }
            SettingsRow("Terms of Use", "\u203A") { onLegal("Terms of Use", LegalTexts.terms) }
            SettingsRow("Open-source licenses", "\u203A") { onLegal("Open-source licenses", LegalTexts.licenses) }
            SettingsRow("About", "v1.3 \u203A") { onLegal("About", LegalTexts.about) }
        }
        Group("CONTACT") {
            SettingsRow("Contact us", "Telegram @blvcknetwork \u203A") { openLink(ctx, "https://t.me/blvcknetwork") }
        }
    }

    StyleDialogs(dlg, s) { dlg = null }
    when (dlg) {
        "theme" -> PickerDialog("Theme", ThemeNames, s.theme, { s.updateTheme(it) }, { dlg = null })
        "after" -> PickerDialog("After convert", listOf("Result screen", "Open PDF directly"), s.afterConvert, { s.setAfter(it) }, { dlg = null })
        "clean" -> PickerDialog(
            "Auto-clean (deletes PDFs older than this, favorites kept)",
            listOf("Off", "30 days", "60 days", "90 days"),
            if (s.autoClean == 0) "Off" else "${s.autoClean} days",
            { s.setClean(if (it == "Off") 0 else it.substringBefore(' ').toInt()) },
            { dlg = null }
        )
        else -> {}
    }
    when (confirm) {
        "history" -> ConfirmDialog("Clear history?", "Only the list is cleared. Your PDF files stay in the folder.", "Clear", onClearHistory) { confirm = null }
        "reset" -> ConfirmDialog("Reset all settings?", "Theme, accent, PDF defaults, folder and behavior go back to defaults.", "Reset", { s.resetAll() }) { confirm = null }
        else -> {}
    }
}
