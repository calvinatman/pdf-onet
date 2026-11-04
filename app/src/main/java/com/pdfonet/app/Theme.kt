package com.pdfonet.app

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

var CBg by mutableStateOf(Color(0xFF121212))
var CCard by mutableStateOf(Color(0xFF1E1E1E))
var CLine by mutableStateOf(Color(0xFF2E2E2E))
var CMuted by mutableStateOf(Color(0xFFA0A0A0))
var CTxt by mutableStateOf(Color(0xFFF2F2F2))
var CNav by mutableStateOf(Color(0xFF262626))
var CAccent by mutableStateOf(Color(0xFFFF7A3D))
val COnAccent = Color(0xFF121212)

fun applyPalette(dark: Boolean, accent: Color) {
    CAccent = accent
    if (dark) {
        CBg = Color(0xFF121212); CCard = Color(0xFF1E1E1E); CLine = Color(0xFF2E2E2E)
        CMuted = Color(0xFFA0A0A0); CTxt = Color(0xFFF2F2F2); CNav = Color(0xFF262626)
    } else {
        CBg = Color(0xFFF6F6F6); CCard = Color(0xFFFFFFFF); CLine = Color(0xFFDDDDDD)
        CMuted = Color(0xFF5F5F5F); CTxt = Color(0xFF141414); CNav = Color(0xFFE6E6E6)
    }
}

@Composable
fun PdfOnetTheme(s: AppSettings, content: @Composable () -> Unit) {
    val dark = when (s.theme) {
        "Dark" -> true
        "Light" -> false
        else -> isSystemInDarkTheme()
    }
    val accent = Color(Accents[s.accent] ?: 0xFFFF7A3DL)
    remember(dark, accent) { applyPalette(dark, accent); 0 }
    val view = LocalView.current
    SideEffect {
        val act = view.context as? Activity
        if (act != null) {
            val c = WindowCompat.getInsetsController(act.window, view)
            c.isAppearanceLightStatusBars = !dark
            c.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = if (dark) darkColorScheme(
            primary = accent, onPrimary = COnAccent, background = CBg, surface = CCard,
            onSurface = CTxt, onBackground = CTxt
        ) else lightColorScheme(
            primary = accent, onPrimary = COnAccent, background = CBg, surface = CCard,
            onSurface = CTxt, onBackground = CTxt
        ),
        content = content
    )
}
