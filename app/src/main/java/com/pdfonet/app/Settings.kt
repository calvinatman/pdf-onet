package com.pdfonet.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

val Accents = linkedMapOf(
    "Orange" to 0xFFFF7A3DL, "Green" to 0xFF3DDC97L, "Blue" to 0xFF5AA9FFL,
    "Purple" to 0xFFC77DFFL, "Pink" to 0xFFFF6FAEL, "Yellow" to 0xFFFFC83DL
)
val TextColors = linkedMapOf(
    "Black" to 0xFF000000L, "Dark gray" to 0xFF333333L, "Blue" to 0xFF0B3D91L,
    "Green" to 0xFF14532DL, "Maroon" to 0xFF7F1D1DL, "White" to 0xFFFFFFFFL
)
val PaperColors = linkedMapOf(
    "White" to 0xFFFFFFFFL, "Cream" to 0xFFFFF8E7L, "Light gray" to 0xFFF0F0F0L,
    "Light blue" to 0xFFEAF3FFL, "Dark" to 0xFF1E1E1EL
)

object Palette {
    fun text(n: String): Int = (TextColors[n] ?: 0xFF000000L).toInt()
    fun paper(n: String): Int = (PaperColors[n] ?: 0xFFFFFFFFL).toInt()
}

data class PdfStyle(
    val pageSize: String = "A4",
    val landscape: Boolean = false,
    val font: String = "Sans",
    val fontSize: Int = 12,
    val margin: String = "Normal",
    val lineSpacing: String = "1.2",
    val align: String = "Left",
    val textColor: String = "Black",
    val paperColor: String = "White",
    val pageNum: String = "1",
    val header: String = "",
    val footer: String = "",
    val watermark: String = "",
    val coverTitle: String = "",
    val coverAuthor: String = "",
    val toc: Boolean = false,
    val splitMode: String = "Off",
    val splitN: Int = 10
) {
    fun marginPt(): Int = when (margin) {
        "Narrow" -> 24
        "Wide" -> 64
        else -> 40
    }
}

class AppSettings(ctx: Context) {
    private val sp = ctx.getSharedPreferences("pdfonet", Context.MODE_PRIVATE)

    var theme by mutableStateOf(sp.getString("theme", "Dark") ?: "Dark")
        private set
    var accent by mutableStateOf(sp.getString("accent", "Orange") ?: "Orange")
        private set
    var afterConvert by mutableStateOf(sp.getString("after", "Result screen") ?: "Result screen")
        private set
    var autoClean by mutableStateOf(sp.getInt("clean", 0))
        private set
    var tree by mutableStateOf<String?>(sp.getString("tree", null))
        private set
    var pro by mutableStateOf(sp.getBoolean("pro", false))
        private set
    var style by mutableStateOf(
        PdfStyle(
            pageSize = sp.getString("pageSize", "A4") ?: "A4",
            landscape = sp.getBoolean("landscape", false),
            font = sp.getString("font", "Sans") ?: "Sans",
            fontSize = sp.getInt("fontSize", 12),
            margin = sp.getString("margin", "Normal") ?: "Normal",
            lineSpacing = sp.getString("lineSpacing", "1.2") ?: "1.2",
            align = sp.getString("align", "Left") ?: "Left",
            textColor = sp.getString("textColor", "Black") ?: "Black",
            paperColor = sp.getString("paperColor", "White") ?: "White",
            pageNum = sp.getString("pageNum", "1") ?: "1"
        )
    )
        private set

    fun setTheme(v: String) { theme = v; sp.edit().putString("theme", v).apply() }
    fun setAccent(v: String) { accent = v; sp.edit().putString("accent", v).apply() }
    fun setAfter(v: String) { afterConvert = v; sp.edit().putString("after", v).apply() }
    fun setClean(v: Int) { autoClean = v; sp.edit().putInt("clean", v).apply() }
    fun setPro(v: Boolean) { pro = v; sp.edit().putBoolean("pro", v).apply() }
    fun setTree(v: String?) {
        tree = v
        val e = sp.edit()
        if (v == null) e.remove("tree") else e.putString("tree", v)
        e.apply()
    }

    fun saveStyle(n: PdfStyle) {
        style = n
        sp.edit()
            .putString("pageSize", n.pageSize).putBoolean("landscape", n.landscape)
            .putString("font", n.font).putInt("fontSize", n.fontSize)
            .putString("margin", n.margin).putString("lineSpacing", n.lineSpacing)
            .putString("align", n.align).putString("textColor", n.textColor)
            .putString("paperColor", n.paperColor).putString("pageNum", n.pageNum)
            .apply()
    }

    fun resetAll() {
        sp.edit().clear().apply()
        theme = "Dark"; accent = "Orange"; afterConvert = "Result screen"
        autoClean = 0; tree = null; pro = false; style = PdfStyle()
    }
}
