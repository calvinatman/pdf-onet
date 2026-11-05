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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

var CBg by mutableStateOf(Color(0xFF121212))
var CCard by mutableStateOf(Color(0xFF1E1E1E))
var CLine by mutableStateOf(Color(0xFF2E2E2E))
var CMuted by mutableStateOf(Color(0xFFA0A0A0))
var CTxt by mutableStateOf(Color(0xFFF2F2F2))
var CNav by mutableStateOf(Color(0xFF262626))
var CAccent by mutableStateOf(Color(0xFFFF7A3D))
var COnAccent by mutableStateOf(Color(0xFF121212))

val ThemeNames = listOf("Dark", "Light", "System", "Black", "Orange", "Red")

private fun contrast(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}

fun isDarkTheme(theme: String, systemDark: Boolean): Boolean = when (theme) {
    "Light" -> false
    "System" -> systemDark
    else -> true
}

fun applyPalette(theme: String, systemDark: Boolean, chosen: Color) {
    val dark = isDarkTheme(theme, systemDark)
    when {
        !dark -> {
            CBg = Color(0xFFF6F6F6); CCard = Color(0xFFFFFFFF); CLine = Color(0xFFDDDDDD)
            CMuted = Color(0xFF5F5F5F); CTxt = Color(0xFF141414); CNav = Color(0xFFE6E6E6)
        }
        theme == "Black" -> {
            CBg = Color(0xFF000000); CCard = Color(0xFF0E0E0E); CLine = Color(0xFF222222)
            CMuted = Color(0xFF9A9A9A); CTxt = Color(0xFFF5F5F5); CNav = Color(0xFF111111)
        }
        theme == "Orange" -> {
            CBg = Color(0xFF1C0F08); CCard = Color(0xFF2A170C); CLine = Color(0xFF4A2A18)
            CMuted = Color(0xFFD2A58A); CTxt = Color(0xFFFFF1E8); CNav = Color(0xFF331B0E)
        }
        theme == "Red" -> {
            CBg = Color(0xFF1E0A0C); CCard = Color(0xFF2C1013); CLine = Color(0xFF54212A)
            CMuted = Color(0xFFD6A0A6); CTxt = Color(0xFFFFECEE); CNav = Color(0xFF36141A)
        }
        else -> {
            CBg = Color(0xFF121212); CCard = Color(0xFF1E1E1E); CLine = Color(0xFF2E2E2E)
            CMuted = Color(0xFFA0A0A0); CTxt = Color(0xFFF2F2F2); CNav = Color(0xFF262626)
        }
    }
    var acc = chosen
    if (contrast(acc, CBg) < 2.2f) acc = if (dark) Color.White else Color.Black
    CAccent = acc
    COnAccent = if (acc.luminance() > 0.4f) Color(0xFF121212) else Color.White
}

@Composable
fun PdfOnetTheme(s: AppSettings, content: @Composable () -> Unit) {
    val sysDark = isSystemInDarkTheme()
    val dark = isDarkTheme(s.theme, sysDark)
    val chosen = Color(Accents[s.accent] ?: 0xFFFF7A3DL)
    remember(s.theme, sysDark, s.accent) { applyPalette(s.theme, sysDark, chosen); 0 }
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
            primary = CAccent, onPrimary = COnAccent, background = CBg, surface = CCard,
            onSurface = CTxt, onBackground = CTxt
        ) else lightColorScheme(
            primary = CAccent, onPrimary = COnAccent, background = CBg, surface = CCard,
            onSurface = CTxt, onBackground = CTxt
        ),
        content = content
    )
}
