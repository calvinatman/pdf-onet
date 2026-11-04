package com.pdfonet.app

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

object Fonts {
    private val sys = linkedMapOf(
        "Sans" to "sans-serif", "Serif" to "serif", "Monospace" to "monospace",
        "Cursive" to "cursive", "Condensed" to "sans-serif-condensed", "Light" to "sans-serif-light"
    )
    private val bundled = linkedMapOf(
        "Roboto" to "Roboto.ttf", "Merriweather" to "Merriweather.ttf",
        "Noto Sans Devanagari" to "NotoSansDevanagari.ttf", "Poppins" to "Poppins.ttf",
        "Lora" to "Lora.ttf", "Roboto Mono" to "RobotoMono.ttf"
    )
    private val cache = HashMap<String, Typeface>()

    private fun dir(ctx: Context) = File(ctx.filesDir, "fonts").apply { mkdirs() }
    fun isCustom(n: String) = n.endsWith(" (custom)")

    fun names(ctx: Context): List<String> {
        val l = ArrayList<String>(sys.keys)
        val have = try { ctx.assets.list("fonts")?.toSet() ?: emptySet() } catch (e: Exception) { emptySet() }
        bundled.forEach { (n, f) -> if (f in have) l.add(n) }
        dir(ctx).listFiles()?.sortedBy { it.name }?.forEach { l.add(it.nameWithoutExtension + " (custom)") }
        return l
    }

    fun typeface(ctx: Context, name: String): Typeface = cache.getOrPut(name) {
        try {
            when {
                sys.containsKey(name) -> Typeface.create(sys[name], Typeface.NORMAL)
                bundled.containsKey(name) -> Typeface.createFromAsset(ctx.assets, "fonts/" + bundled[name])
                isCustom(name) -> {
                    val stem = name.removeSuffix(" (custom)")
                    val f = dir(ctx).listFiles()?.firstOrNull { it.nameWithoutExtension == stem } ?: error("missing")
                    Typeface.createFromFile(f)
                }
                else -> Typeface.SANS_SERIF
            }
        } catch (e: Exception) {
            Typeface.SANS_SERIF
        }
    }

    fun importFont(ctx: Context, uri: Uri): String? {
        return try {
            var name = ""
            ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (i >= 0) name = c.getString(i) ?: ""
                }
            }
            val ext = name.substringAfterLast('.', "").lowercase()
            if (ext != "ttf" && ext != "otf") return null
            val stem = name.substringBeforeLast('.').replace(Regex("[^A-Za-z0-9 _-]"), "").trim().ifBlank { "font" }
            val f = File(dir(ctx), "$stem.$ext")
            ctx.contentResolver.openInputStream(uri)!!.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
            try { Typeface.createFromFile(f) } catch (e: Exception) { f.delete(); return null }
            cache.remove("$stem (custom)")
            "$stem (custom)"
        } catch (e: Exception) {
            null
        }
    }

    fun remove(ctx: Context, name: String) {
        val stem = name.removeSuffix(" (custom)")
        dir(ctx).listFiles()?.filter { it.nameWithoutExtension == stem }?.forEach { it.delete() }
        cache.remove(name)
    }
}
