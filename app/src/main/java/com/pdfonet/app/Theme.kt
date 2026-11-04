package com.pdfonet.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CBg = Color(0xFF121212)
val CCard = Color(0xFF1E1E1E)
val CLine = Color(0xFF2E2E2E)
val CMuted = Color(0xFFA0A0A0)
val CAccent = Color(0xFFFF7A3D)
val CTxt = Color(0xFFF2F2F2)
val CNav = Color(0xFF262626)

@Composable
fun PdfOnetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = CAccent,
            onPrimary = Color(0xFF121212),
            background = CBg,
            surface = CCard,
            onSurface = CTxt,
            onBackground = CTxt
        ),
        content = content
    )
}
