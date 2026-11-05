package com.pdfonet.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Icons24: Map<String, String> = mapOf(
    "doc" to "M6 3h8l4 4v14H6z M9 12h6 M9 16h6",
    "merge" to "M3 5h8v14H3z M13 5h8v14h-8z",
    "split" to "M6 3h12v18H6z M3 12h18",
    "rotate" to "M20 12a8 8 0 1 1-3-6.2 M20 4v5h-5",
    "grid" to "M4 4h7v7H4z M13 4h7v7h-7z M4 13h7v7H4z M13 13h7v7h-7z",
    "pen" to "M4 20l4-1 11-11-3-3L5 16z",
    "hash" to "M9 4L7 20 M17 4l-2 16 M4 9h16 M3 15h16",
    "lock" to "M5 11h14v9H5z M8 11V8a4 4 0 0 1 8 0v3",
    "press" to "M4 4l6 6 M10 5v5H5 M20 20l-6-6 M14 19v-5h5",
    "cam" to "M3 8h18v12H3z M8 8l1.5-3h5L16 8 M15 14a3 3 0 1 1-6 0 3 3 0 0 1 6 0",
    "text" to "M5 6h14 M12 6v13 M9 19h6",
    "img" to "M3 5h18v14H3z M4 18l5-5 4 4 3-3 4 4",
    "crop" to "M7 3v14h14 M3 7h14v14",
    "ocr" to "M4 8V5h3 M20 8V5h-3 M4 16v3h3 M20 16v3h-3 M8 12h8",
    "zip" to "M5 3h14v18H5z M12 3v9 M10 8h4 M10 12h4",
    "flip" to "M12 3v18 M8 7l-5 5 5 5z M16 7l5 5-5 5z",
    "layers" to "M12 4l9 5-9 5-9-5z M3 14l9 5 9-5",
    "redact" to "M4 9h16v6H4z",
    "info" to "M12 3a9 9 0 1 1 0 18 9 9 0 0 1 0-18 M12 11v6 M12 7.5v.5",
    "trash" to "M5 7h14 M9 7V4h6v3 M7 7l1 13h8l1-13",
    "tpl" to "M4 4h16v16H4z M4 10h16 M10 10v10",
    "undo" to "M9 14L4 9l5-5 M4 9h10a6 6 0 0 1 0 12h-3",
    "redo" to "M15 14l5-5-5-5 M20 9H10a6 6 0 0 0 0 12h3",
    "find" to "M18 11a7 7 0 1 1-14 0 7 7 0 0 1 14 0 M20 20l-4-4",
    "search" to "M18 11a7 7 0 1 1-14 0 7 7 0 0 1 14 0 M20 20l-4-4",
    "full" to "M4 9V4h5 M20 9V4h-5 M4 15v5h5 M20 15v5h-5",
    "exit" to "M9 4v5H4 M15 4v5h5 M9 20v-5H4 M15 20v-5h5",
    "home" to "M3 11L12 3l9 8 M5 10v10h14V10 M10 20v-6h4v6",
    "folder" to "M3 6h6l2 2h10v11H3z",
    "gear" to "M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0 M20 12a8 8 0 1 1-16 0 8 8 0 0 1 16 0",
    "star" to "M12 3l2.7 5.6 6.1.8-4.5 4.3 1.1 6.1L12 17l-5.4 2.8 1.1-6.1L3.2 9.4l6.1-.8z",
    "chevL" to "M15 5l-7 7 7 7",
    "chevR" to "M9 5l7 7-7 7",
    "check" to "M5 12l5 5 9-10",
    "close" to "M6 6l12 12 M18 6L6 18",
    "arrow" to "M5 12h14 M13 6l6 6-6 6"
)

private val pathCache = HashMap<String, Path>()

fun iconPath(name: String): Path = pathCache.getOrPut(name) {
    PathParser().parsePathString(Icons24[name] ?: "M0 0").toPath()
}

@Composable
fun SvgIcon(name: String, color: Color, dim: Dp = 24.dp, filled: Boolean = false) {
    val path = iconPath(name)
    val dashed = name == "gear"
    Canvas(Modifier.size(dim)) {
        val k = this.size.width / 24f
        withTransform({ scale(k, k, Offset.Zero) }) {
            if (filled || name == "redact") {
                drawPath(path, color, style = Fill)
            } else {
                drawPath(
                    path, color,
                    style = Stroke(
                        width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round,
                        pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(3f, 2.5f)) else null
                    )
                )
            }
        }
    }
}

@Composable
fun IconBtnTint(name: String, label: String, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) { SvgIcon(name, tint, 22.dp) }
}

@Composable
fun IconBtn(name: String, label: String, onClick: () -> Unit) {
    IconBtnTint(name, label, CTxt, onClick)
}

@Composable
fun IconGroup(onUndo: () -> Unit, onRedo: () -> Unit, onFind: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Row(Modifier.clip(shape).background(CCard).border(1.dp, CLine, shape).padding(2.dp)) {
        IconBtn("undo", "Undo", onUndo)
        IconBtn("redo", "Redo", onRedo)
        IconBtn("find", "Find", onFind)
    }
}

@Composable
fun FilterChipX(label: String, on: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier.heightIn(min = 44.dp).clip(shape)
            .background(if (on) CAccent else CCard)
            .border(1.dp, if (on) CAccent else CLine, shape)
            .clickable(onClick = onClick).padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, color = if (on) COnAccent else CTxt, fontSize = 14.sp,
            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun Wordmark(h: Dp) {
    val onDark = CBg.luminance() < 0.5f
    val img: @Composable () -> Unit = {
        Image(
            painter = painterResource(R.drawable.wordmark),
            contentDescription = "PDio",
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(h).aspectRatio(420f / 196f)
        )
    }
    if (onDark) {
        img()
    } else {
        Box(
            Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF121212))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) { img() }
    }
}

@Composable
fun rememberHaptic(s: AppSettings): () -> Unit {
    val h = LocalHapticFeedback.current
    return { if (s.haptic) h.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
}
